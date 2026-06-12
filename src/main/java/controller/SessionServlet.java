package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for Session management (Save/Load).
 * Extends BaseServlet to leverage common JSON processing and N-Tier architecture.
 * 
 * @author Developer
 */
@WebServlet("/api/sessions")
public class SessionServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");

        try {
            if (id == null || id.isEmpty()) {
                // List all sessions via Service
                List<Session> sessions = service.getAllSessions();
                ArrayNode array = mapper.createArrayNode();
                for (Session s : sessions) {
                    ObjectNode node = mapper.createObjectNode();
                    node.put("id", s.getId());
                    node.put("name", s.getName());
                    node.put("createdAt", s.getCreatedAt().toString());
                    array.add(node);
                }
                sendJsonResponse(response, array);
            } else {
                // Load specific session via Service
                Session session = service.loadSession(id);
                if (session == null) {
                    sendError(response, "Session not found", HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // Map Session back to the flat JSON format expected by the frontend
                ObjectNode root = mapper.createObjectNode();
                root.put("colCount", session.getCriteria().size());
                root.put("rowCount", session.getAlternatives().size());

                ObjectNode data = mapper.createObjectNode();

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
                sendJsonResponse(response, root);
            }
        } catch (SQLException e) {
            sendError(response, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String body = getRequestBody(request);
            if (body.trim().isEmpty()) {
                sendError(response, "Empty payload", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            JsonNode rootNode = mapper.readTree(body);
            String sessionName = rootNode.has("sessionName") ? rootNode.get("sessionName").asText() : "New Session";
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null) {
                sendError(response, "Missing data object", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Delegate everything to the Service
            Session session = service.saveSession(sessionName, dataNode);

            ObjectNode res = mapper.createObjectNode();
            res.put("status", "success");
            res.put("id", session.getId());
            sendJsonResponse(response, res);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
