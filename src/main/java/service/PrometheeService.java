package service;

import com.fasterxml.jackson.databind.JsonNode;
import dao.SessionDAO;
import model.*;
import model.function.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Service class that handles the core business logic for PROMETHEE calculations.
 * This class acts as a bridge between the Controllers (Servlets) and the Models/DAO.
 */
public class PrometheeService {

    private final PrometheeCalcul calculationEngine = new PrometheeCalcul();
    private final SessionDAO sessionDAO = new SessionDAO();

    /**
     * Orchestrates the full PROMETHEE II calculation and returns the full result (flows + matrix).
     */
    public CalculationResult processFullCalculation(JsonNode node) {
        ArrayList<Criterion> criteria = extractCriteria(node);
        ArrayList<Alternative> alternatives = extractAlternatives(node, criteria);
        double[][] matrix = new double[0][0];

        if (alternatives.size() >= 2) {
            matrix = calculationEngine.calculate(alternatives, criteria);
        }
        return new CalculationResult(alternatives, matrix);
    }

    /**
     * Legacy method for simple list return.
     */
    public List<Alternative> processCalculation(JsonNode node) {
        return processFullCalculation(node).getAlternatives();
    }

    public Session saveSession(String sessionName, JsonNode dataNode) throws SQLException {
        List<Criterion> criteria = extractCriteria(dataNode);
        List<Alternative> alternatives = extractAlternatives(dataNode, criteria);

        if (alternatives.size() >= 2) {
            calculationEngine.calculate(alternatives, criteria);
        }

        Session session = new Session();
        session.setName(sessionName);
        session.setCriteria(criteria);
        session.setAlternatives(alternatives);

        sessionDAO.saveSession(session);
        return session;
    }

    public Session loadSession(String sessionId) throws SQLException {
        return sessionDAO.loadSession(sessionId);
    }

    public List<Session> getAllSessions() throws SQLException {
        return sessionDAO.getAllSessions();
    }

    public ArrayList<Criterion> extractCriteria(JsonNode node) {
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
            PreferenceFunction func = createFunction(type, getDoubleSafe(node, "p_" + i), getDoubleSafe(node, "q_" + i), getDoubleSafe(node, "s_" + i));
            list.add(new Criterion(name, weight, isMax, func));
        }
        return list;
    }

    public ArrayList<Alternative> extractAlternatives(JsonNode node, List<Criterion> criteria) {
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
        Collections.sort(critIndices);

        for (int i : indices) {
            String nameField = "altName_" + i;
            String name = node.has(nameField) ? node.get(nameField).asText() : "Alternative " + i;
            Alternative alt = new Alternative(UUID.randomUUID().toString(), name);
            for (int j = 0; j < criteria.size(); j++) {
                int critIndex = critIndices.get(j);
                alt.addValue(criteria.get(j), getDoubleSafe(node, "val_" + i + "_" + critIndex));
            }
            list.add(alt);
        }
        return list;
    }

    private PreferenceFunction createFunction(String type, double p, double q, double s) {
        switch (type) {
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
            JsonNode fn = node.get(field);
            if (fn.isNumber()) return fn.asDouble();
            String t = fn.asText();
            if (t == null || t.trim().isEmpty()) return 0.0;
            try { return Double.parseDouble(t.replace(',', '.')); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }
}
