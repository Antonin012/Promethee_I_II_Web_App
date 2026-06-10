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
import model.Alternative;
import service.CalculationResult;
import service.PrometheeService;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Controller for PROMETHEE II calculations.
 * Adheres to the MVC pattern by delegating business logic to PrometheeService.
 */
@WebServlet("/calculate")
public class PrometheeServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PrometheeService prometheeService = new PrometheeService();

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
                out.print("{\"error\": \"Empty request body\"}");
                return;
            }

            JsonNode node = objectMapper.readTree(body);

            // Correctly delegate calculation to service and get FULL results (A + Matrix)
            CalculationResult result = prometheeService.processFullCalculation(node);

            if (result.getAlternatives().isEmpty()) {
                 out.print("[]");
                 return;
            }

            // Build JSON response using the data returned by the service
            ObjectNode root = objectMapper.createObjectNode();
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

            out.print(root.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
