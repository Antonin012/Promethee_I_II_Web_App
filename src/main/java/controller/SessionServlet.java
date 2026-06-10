package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.*;
import service.PrometheeService;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for Session management (Save/Load).
 * Adheres to the strict N-Tier architecture by delegating business logic and persistence 
 * operations to the PrometheeService.
 * 
 * @author Developer
 */
@WebServlet("/api/sessions")
public class SessionServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PrometheeService prometheeService = new PrometheeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String id = request.getParameter("id");

        try {
            if (id == null || id.isEmpty()) {
                // List all sessions via Service
                List<Session> sessions = prometheeService.getAllSessions();
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
                // Load specific session via Service
                Session session = prometheeService.loadSession(id);
                if (session == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"Session not found\"}");
                    return;
                }

                // Map Session back to the flat JSON format expected by the frontend
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
                    if (pf instanceof model.function.UshapeFunction) {
                        funcType = "type2";
                        data.put("q_" + cIndex, ((model.function.UshapeFunction) pf).getQ());
                    } else if (pf instanceof model.function.VShapeFunction) {
                        funcType = "type3";
                        data.put("p_" + cIndex, ((model.function.VShapeFunction) pf).getP());
                    } else if (pf instanceof model.function.LevelFunction) {
                        funcType = "type4";
                        data.put("q_" + cIndex, ((model.function.LevelFunction) pf).getQ());
                        data.put("p_" + cIndex, ((model.function.LevelFunction) pf).getP());
                    } else if (pf instanceof model.function.VShapeIndifferences) {
                        funcType = "type5";
                        data.put("q_" + cIndex, ((model.function.VShapeIndifferences) pf).getQ());
                        data.put("p_" + cIndex, ((model.function.VShapeIndifferences) pf).getP());
                    } else if (pf instanceof model.function.GaussianFunction) {
                        funcType = "type6";
                        data.put("s_" + cIndex, ((model.function.GaussianFunction) pf).getS());
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
            String sessionName = rootNode.has("sessionName") ? rootNode.get("sessionName").asText() : "New Session";
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Missing data object\"}");
                return;
            }

            // Delegate everything to the Service (extraction, calculation, and saving)
            Session session = prometheeService.saveSession(sessionName, dataNode);

            out.print("{\"status\": \"success\", \"id\": \"" + session.getId() + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
