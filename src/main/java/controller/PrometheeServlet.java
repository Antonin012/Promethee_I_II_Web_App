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

@WebServlet("/calculate")
public class PrometheeServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Lecture brute du corps de la requête
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

            if (alternatives.size() >= 2) {
                PrometheeCalcul engine = new PrometheeCalcul();
                engine.calculate(alternatives, criteria);
                System.out.println("Calcul effectué avec succès.");
            } else {
                System.out.println("Calcul sauté : pas assez d'alternatives.");
            }

            // Parse result to JSON
            String jsonResponse = objectMapper.writeValueAsString(alternatives);

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

    private ArrayList<Criterion> extractCriteria(JsonNode node) {
        ArrayList<Criterion> list = new ArrayList<>();
        // On récupère tous les indices de critères présents dans le JSON
        java.util.Set<Integer> indices = new java.util.TreeSet<>();
        node.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("critName_")) {
                try {
                    indices.add(Integer.parseInt(name.substring(9)));
                } catch (NumberFormatException ignored) {}
            }
        });

        for (int i : indices) {
            String name = node.get("critName_" + i).asText();
            double weight = getDoubleSafe(node, "weight_" + i);
            boolean isMax = node.get("isMax_" + i).asBoolean() || "true".equals(node.get("isMax_" + i).asText());
            String type = node.get("func_" + i).asText();
            
            double p = getDoubleSafe(node, "p_" + i);
            double q = getDoubleSafe(node, "q_" + i);
            double s = getDoubleSafe(node, "s_" + i);

            PreferenceFunction func = createFunction(type, p, q, s);
            list.add(new Criterion(name, weight, isMax, func));
        }
        return list;
    }

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
            String name = node.get("altName_" + i).asText();
            Alternative alt = new Alternative(String.valueOf(i), name);
            
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
