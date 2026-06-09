package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;

/**
 * Servlet handling POST requests for calculating PROMETHEE outranking flows and matrices.
 * It parses the incoming JSON data to extract criteria and alternatives,
 * computes the PROMETHEE matrices, and returns the results in JSON format.
 * 
 * @author Developer
 */
@WebServlet("/calculate")
public class PrometheeServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles POST requests for PROMETHEE calculation.
     * Parses the JSON body to retrieve criteria and alternative definitions, performs the calculation,
     * and writes the resulting matrix and updated alternatives back as JSON.
     * 
     * @param request the HttpServletRequest containing the JSON payload
     * @param response the HttpServletResponse where the JSON results are sent
     * @throws ServletException if an error occurs during servlet processing
     * @throws IOException if an I/O error occurs during request/response handling
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            byte[] bytes = request.getInputStream().readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (body.trim().isEmpty()) {
                System.out.println("Empty JSON");
                response.getWriter().print("[]");
                return;
            }

            JsonNode rootNode = objectMapper.readTree(body);
            ArrayList<Criterion> criteria = extractCriteria(rootNode);
            ArrayList<Alternative> alternatives = extractAlternatives(rootNode, criteria);

            double[][] matrix = new double[0][0];
            if (alternatives.size() >= 2) {
                PrometheeCalcul engine = new PrometheeCalcul();
                matrix = engine.calculate(alternatives, criteria);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("{\"error\": \"At least two alternatives are required for calculation.\"}");
                return;
            }

            // Create a wrapper object for the response
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("alternatives", alternatives);
            result.put("matrix", matrix);

            String jsonResponse = objectMapper.writeValueAsString(result);

            PrintWriter out = response.getWriter();
            out.print(jsonResponse);
            out.flush();

        } catch (Exception e) {
            System.out.println("Error Servlet : " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Extracts a list of Criterion objects from a given JSON node.
     * 
     * @param node the root JsonNode containing the criteria configuration
     * @return an ArrayList of Criterion objects extracted from the JSON
     */
    private ArrayList<Criterion> extractCriteria(JsonNode node) {
        ArrayList<Criterion> list = new ArrayList<>();
        java.util.Set<Integer> indices = new java.util.TreeSet<>();
        node.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("critName_")) {
                try {
                    indices.add(Integer.parseInt(name.substring(9)));
                } catch (NumberFormatException ignored) {}
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

            String type = "type1";
            if (node.has("func_" + i)) {
                type = node.get("func_" + i).asText();
            }
            
            double p = getDoubleSafe(node, "p_" + i);
            double q = getDoubleSafe(node, "q_" + i);
            double s = getDoubleSafe(node, "s_" + i);

            try {
                PreferenceFunction func = createFunction(type, p, q, s);
                list.add(new Criterion(name, weight, isMax, func));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Criterion '" + name + "': " + e.getMessage());
            }
        }
        return list;
    }

    /**
     * Extracts a list of Alternative objects from a given JSON node, applying evaluations based on provided criteria.
     * 
     * @param node the root JsonNode containing the alternatives and their evaluations
     * @param criteria the list of available Criteria to map the evaluations to
     * @return an ArrayList of Alternative objects fully initialized with their evaluations
     */
    private ArrayList<Alternative> extractAlternatives(JsonNode node, ArrayList<Criterion> criteria) {
        ArrayList<Alternative> list = new ArrayList<>();
        java.util.Set<Integer> indices = new java.util.TreeSet<>();
        node.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("altName_")) {
                try {
                    indices.add(Integer.parseInt(name.substring(8)));
                } catch (NumberFormatException ignored) {}
            }
        });

        java.util.ArrayList<Integer> critIndices = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("critName_")) {
                try {
                    critIndices.add(Integer.parseInt(name.substring(9)));
                } catch (NumberFormatException ignored) {}
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
     * Instantiates a PreferenceFunction based on its string type identifier and optional threshold parameters.
     * 
     * @param type a string identifier for the preference function type (e.g., "type1")
     * @param p the strict preference threshold
     * @param q the indifference threshold
     * @param s the standard deviation-like threshold for the Gaussian function
     * @return an instantiated PreferenceFunction object corresponding to the given type
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
     * Safely retrieves a double value from a JsonNode field, parsing string approximations if necessary.
     * 
     * @param node the JsonNode containing the target field
     * @param field the name of the field to read
     * @return the extracted double value, or 0.0 if the field is missing or invalid
     */
    private double getDoubleSafe(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            JsonNode fieldNode = node.get(field);
            if (fieldNode.isNumber()) {
                return fieldNode.asDouble();
            } else {
                String text = fieldNode.asText();
                if (text == null || text.trim().isEmpty()) return 0.0;
                try {
                    return Double.parseDouble(text.replace(',', '.'));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }
        return 0.0;
    }
}