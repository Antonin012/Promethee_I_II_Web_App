package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Alternative;
import model.CalculationResult;

import java.io.IOException;

/**
 * Controller for PROMETHEE II calculations.
 * Extends BaseServlet to benefit from centralized JSON and error handling.
 * 
 * @author Developer
 */
@WebServlet("/calculate")
public class PrometheeServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String body = getRequestBody(request);
            if (body.trim().isEmpty()) {
                sendError(response, "Empty request body", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            JsonNode node = mapper.readTree(body);

            // Delegate calculation to service
            CalculationResult result = service.processFullCalculation(node);

            if (result.getAlternatives().isEmpty()) {
                 response.getWriter().print("[]");
                 return;
            }

            // Build JSON response
            ObjectNode root = mapper.createObjectNode();
            ArrayNode altArray = root.putArray("alternatives");
            for (Alternative alt : result.getAlternatives()) {
                altArray.addPOJO(alt);
            }
            
            ArrayNode matrixArray = root.putArray("matrix");
            for (double[] row : result.getMatrix()) {
                ArrayNode rowArray = matrixArray.addArray();
                for (double val : row) {
                    rowArray.add(val);
                }
            }

            // Include GAIA plane data if available
            if (result.getGaiaData() != null && !result.getGaiaData().isEmpty()) {
                root.putPOJO("gaia", result.getGaiaData());
            }

            sendJsonResponse(response, root);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
