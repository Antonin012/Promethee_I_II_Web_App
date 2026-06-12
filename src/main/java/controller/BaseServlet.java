package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.PrometheeService;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for all Servlets in the application.
 * Centralizes common boilerplate code for JSON handling and error management.
 * 
 * @author Developer
 */
public abstract class BaseServlet extends HttpServlet {

    protected final ObjectMapper mapper = new ObjectMapper();
    protected final PrometheeService service = new PrometheeService();

    /**
     * Utility method to read the raw body of an HTTP request.
     */
    protected String getRequestBody(HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Utility method to send a successful JSON response.
     */
    protected void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(mapper.valueToTree(data).toString());
    }

    /**
     * Utility method to send an error response in JSON format.
     */
    protected void sendError(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print("{\"error\": \"" + message.replace("\"", "\\\"") + "\"}");
    }
}
