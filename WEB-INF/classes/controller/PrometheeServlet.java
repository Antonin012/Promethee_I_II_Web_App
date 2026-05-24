package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.*;
import model.function.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.stream.Collectors;

@WebServlet("/calculate")
public class PrometheeServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {

            ArrayList<Criterion> criteria = extractCriteria(request);
            ArrayList<Alternative> alternatives = extractAlternatives(request, criteria);

            PrometheeCalcul engine = new PrometheeCalcul();
            engine.calculate(alternatives, criteria);

            String jsonResponse = buildJsonResponse(alternatives);

            PrintWriter out = response.getWriter();
            out.print(jsonResponse);
            out.flush();

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private ArrayList<Criterion> extractCriteria(HttpServletRequest request) {
        ArrayList<Criterion> list = new ArrayList<>();
        int i = 1;
        while (request.getParameter("critName_" + i) != null) {
            String name = request.getParameter("critName_" + i);
            double weight = Double.parseDouble(request.getParameter("weight_" + i));
            boolean isMax = Boolean.parseBoolean(request.getParameter("isMax_" + i));
            String type = request.getParameter("func_" + i);
            
            double p = parseDoubleSafe(request.getParameter("p_" + i));
            double q = parseDoubleSafe(request.getParameter("q_" + i));
            double s = parseDoubleSafe(request.getParameter("s_" + i));

            PreferenceFunction func = createFunction(type, p, q, s);
            list.add(new Criterion(name, weight, isMax, func));
            i++;
        }
        return list;
    }

    private ArrayList<Alternative> extractAlternatives(HttpServletRequest request, ArrayList<Criterion> criteria) {
        ArrayList<Alternative> list = new ArrayList<>();
        int i = 1;
        while (request.getParameter("altName_" + i) != null) {
            String name = request.getParameter("altName_" + i);
            Alternative alt = new Alternative(String.valueOf(i), name);
            
            for (int j = 0; j < criteria.size(); j++) {
                double val = Double.parseDouble(request.getParameter("val_" + i + "_" + (j + 1)));
                alt.addValue(criteria.get(j), val);
            }
            list.add(alt);
            i++;
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

    private double parseDoubleSafe(String val) {
        if (val == null || val.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String buildJsonResponse(ArrayList<Alternative> alternatives) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < alternatives.size(); i++) {
            Alternative a = alternatives.get(i);
            sb.append("{");
            sb.append("\"name\":\"").append(a.getName()).append("\",");
            sb.append("\"phiPlus\":").append(a.getPhiPlus()).append(",");
            sb.append("\"phiMinus\":").append(a.getPhiMinus()).append(",");
            sb.append("\"phiNet\":").append(a.getPhiNet());
            sb.append("}");
            if (i < alternatives.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
