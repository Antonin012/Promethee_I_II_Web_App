package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dao.SessionDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.*;
import model.function.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Servlet handling REST API requests for managing decision-making sessions.
 * Supports GET for retrieving a list of sessions or details of a specific session,
 * and POST for saving a newly calculated session.
 * 
 * @author Developer
 */
@WebServlet("/api/sessions")
public class SessionServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionDAO sessionDAO = new SessionDAO();

    /**
     * Handles GET requests. 
     * If the 'id' parameter is absent, retrieves a list of all saved sessions.
     * If the 'id' parameter is present, retrieves the complete details of the specified session
     * and maps it to the frontend JSON structure.
     * 
     * @param request the HttpServletRequest containing query parameters
     * @param response the HttpServletResponse used to write the JSON response
     * @throws ServletException if an error occurs during servlet processing
     * @throws IOException if an I/O error occurs during request/response handling
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String id = request.getParameter("id");

        try {
            if (id == null || id.isEmpty()) {
                List<Session> sessions = sessionDAO.getAllSessions();
                ArrayNode array = objectMapper.createArrayNode();
                for (Session s : sessions) {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("id", s.getId());
                    node.put("name", s.getName());
                    node.put("createdAt", s.getCreatedAt().toString());
                    array.add(node);
                }
                out.print(array.toString());
            } else {
                Session session = sessionDAO.loadSession(id);
                if (session == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"Session not found\"}");
                    return;
                }

                ObjectNode root = objectMapper.createObjectNode();
                root.put("colCount", session.getCriteria().size());
                root.put("rowCount", session.getAlternatives().size());

                ObjectNode data = objectMapper.createObjectNode();

                int cIndex = 1;
                for (Criterion c : session.getCriteria()) {
                    data.put("critName_" + cIndex, c.getName());
                    data.put("weight_" + cIndex, c.getWeight());
                    data.put("isMax_" + cIndex, String.valueOf(c.isMaximize()));

                    String funcType = "type1";
                    PreferenceFunction pf = c.getPreferenceFunction();
                    if (pf instanceof UshapeFunction) {
                        funcType = "type2";
                        data.put("q_" + cIndex, ((UshapeFunction) pf).getQ());
                    } else if (pf instanceof VShapeFunction) {
                        funcType = "type3";
                        data.put("p_" + cIndex, ((VShapeFunction) pf).getP());
                    } else if (pf instanceof LevelFunction) {
                        funcType = "type4";
                        data.put("q_" + cIndex, ((LevelFunction) pf).getQ());
                        data.put("p_" + cIndex, ((LevelFunction) pf).getP());
                    } else if (pf instanceof VShapeIndifferences) {
                        funcType = "type5";
                        data.put("q_" + cIndex, ((VShapeIndifferences) pf).getQ());
                        data.put("p_" + cIndex, ((VShapeIndifferences) pf).getP());
                    } else if (pf instanceof GaussianFunction) {
                        funcType = "type6";
                        data.put("s_" + cIndex, ((GaussianFunction) pf).getS());
                    }
                    data.put("func_" + cIndex, funcType);
                    cIndex++;
                }

                int aIndex = 1;
                for (Alternative alt : session.getAlternatives()) {
                    data.put("altName_" + aIndex, alt.getName());
                    int acIndex = 1;
                    for (Criterion c : session.getCriteria()) {
                        data.put("val_" + aIndex + "_" + acIndex, alt.getValue(c));
                        acIndex++;
                    }
                    aIndex++;
                }

                root.set("data", data);
                out.print(root.toString());
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Handles POST requests. 
     * Parses the incoming JSON configuration, calculates PROMETHEE outranking flows,
     * creates a Session object, and delegates saving it to the database via SessionDAO.
     * 
     * @param request the HttpServletRequest containing the JSON payload
     * @param response the HttpServletResponse used to write the JSON response status
     * @throws ServletException if an error occurs during servlet processing
     * @throws IOException if an I/O error occurs during request/response handling
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            byte[] bytes = request.getInputStream().readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (body.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Empty payload\"}");
                return;
            }

            JsonNode rootNode = objectMapper.readTree(body);
            String sessionName = rootNode.has("sessionName") ? rootNode.get("sessionName").asText() : "Nouvelle Session";
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Missing data object\"}");
                return;
            }

            ArrayList<Criterion> criteria = extractCriteria(dataNode);
            ArrayList<Alternative> alternatives = extractAlternatives(dataNode, criteria);

            if (alternatives.size() >= 2) {
                PrometheeCalcul engine = new PrometheeCalcul();
                engine.calculate(alternatives, criteria); // Updates alternatives with phi flows
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"At least two alternatives are required.\"}");
                return;
            }

            Session session = new Session();
            session.setName(sessionName);
            session.setCriteria(criteria);
            session.setAlternatives(alternatives);

            sessionDAO.saveSession(session);

            out.print("{\"status\": \"success\", \"id\": \"" + session.getId() + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Extracts a list of Criterion objects from a given JSON node.
     * 
     * @param node the JsonNode containing the criteria configuration
     * @return an ArrayList of Criterion objects
     */
    private ArrayList<Criterion> extractCriteria(JsonNode node) {
        ArrayList<Criterion> list = new ArrayList<>();
        Set<Integer> indices = new TreeSet<>();
        node.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("critName_")) {
                try { indices.add(Integer.parseInt(name.substring(9))); } catch (NumberFormatException ignored) {}
            }
        });

        for (int i : indices) {
            String nameField = "critName_" + i;
            String name = node.has(nameField) ? node.get(nameField).asText() : "Criterion " + i;
            double weight = getDoubleSafe(node, "weight_" + i);
            boolean isMax = false;
            if (node.has("isMax_" + i)) {
                JsonNode maxNode = node.get("isMax_" + i);
                isMax = maxNode.asBoolean() || "true".equals(maxNode.asText());
            }

            String type = node.has("func_" + i) ? node.get("func_" + i).asText() : "type1";
            double p = getDoubleSafe(node, "p_" + i);
            double q = getDoubleSafe(node, "q_" + i);
            double s = getDoubleSafe(node, "s_" + i);

            PreferenceFunction func = createFunction(type, p, q, s);
            list.add(new Criterion(name, weight, isMax, func));
        }
        return list;
    }

    /**
     * Extracts a list of Alternative objects from a given JSON node, applying evaluations.
     * 
     * @param node the JsonNode containing the alternatives
     * @param criteria the list of corresponding Criteria
     * @return an ArrayList of initialized Alternative objects
     */
    private ArrayList<Alternative> extractAlternatives(JsonNode node, ArrayList<Criterion> criteria) {
        ArrayList<Alternative> list = new ArrayList<>();
        Set<Integer> indices = new TreeSet<>();
        node.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("altName_")) {
                try { indices.add(Integer.parseInt(name.substring(8))); } catch (NumberFormatException ignored) {}
            }
        });

        ArrayList<Integer> critIndices = new ArrayList<>();
        node.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("critName_")) {
                try { critIndices.add(Integer.parseInt(name.substring(9))); } catch (NumberFormatException ignored) {}
            }
        });
        java.util.Collections.sort(critIndices);

        for (int i : indices) {
            String nameField = "altName_" + i;
            String name = node.has(nameField) ? node.get(nameField).asText() : "Alternative " + i;
            Alternative alt = new Alternative(java.util.UUID.randomUUID().toString(), name);
            
            for (int j = 0; j < criteria.size(); j++) {
                int critIndex = critIndices.get(j);
                String key = "val_" + i + "_" + critIndex;
                double val = getDoubleSafe(node, key);
                alt.addValue(criteria.get(j), val);
            }
            list.add(alt);
        }
        return list;
    }

    /**
     * Instantiates a PreferenceFunction based on its type string and threshold parameters.
     * 
     * @param type the preference function type string
     * @param p the strict preference threshold
     * @param q the indifference threshold
     * @param s the standard deviation-like threshold
     * @return the instantiated PreferenceFunction
     */
    private PreferenceFunction createFunction(String type, double p, double q, double s) {
        switch (type) {
            case "type1": return new UsualFunction();
            case "type2": return new UshapeFunction(q);
            case "type3": return new VShapeFunction(p);
            case "type4": return new LevelFunction(q, p);
            case "type5": return new VShapeIndifferences(q, p);
            case "type6": return new GaussianFunction(s);
            default: return new UsualFunction();
        }
    }

    /**
     * Safely retrieves a double value from a JsonNode field.
     * 
     * @param node the JsonNode containing the target field
     * @param field the name of the field to read
     * @return the extracted double value, or 0.0 if invalid
     */
    private double getDoubleSafe(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            JsonNode fieldNode = node.get(field);
            if (fieldNode.isNumber()) return fieldNode.asDouble();
            String text = fieldNode.asText();
            if (text == null || text.trim().isEmpty()) return 0.0;
            try { return Double.parseDouble(text.replace(',', '.')); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }
}