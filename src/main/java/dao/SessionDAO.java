package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Alternative;
import model.Criterion;
import model.PreferenceFunction;
import model.Session;
import model.function.GaussianFunction;
import model.function.LevelFunction;
import model.function.UshapeFunction;
import model.function.UsualFunction;
import model.function.VShapeFunction;
import model.function.VShapeIndifferences;

/**
 * Data Access Object for handling Session entities.
 * Provides methods to save, load, and retrieve decision-making sessions from the PostgreSQL database.
 * 
 * @author Developer
 */
public class SessionDAO {

    private static final String URL = "jdbc:postgresql://database_postgres:5432/" + System.getenv("POSTGRES_DB");
    private static final String USER = System.getenv("POSTGRES_USER");
    private static final String PASSWORD = System.getenv("POSTGRES_PASSWORD");

    /**
     * Establishes and returns a connection to the PostgreSQL database.
     * 
     * @return a Connection object
     * @throws SQLException if a database access error occurs or the driver is not found
     */
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Saves a complete session, including its criteria, alternatives, and evaluations, 
     * into the database within a single transaction.
     * 
     * @param session the Session object containing all data to persist
     * @throws SQLException if a database access error occurs or the transaction fails
     */
    public void saveSession(Session session) throws SQLException {
        String insertSessionSql = "INSERT INTO session (id, name, created_at) VALUES (?, ?, ?)";
        String insertCriterionSql = "INSERT INTO criterion (id, session_id, name, weight, is_maximize, function_type, param_p, param_q, param_s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertAlternativeSql = "INSERT INTO alternative (id, session_id, name) VALUES (?, ?, ?)";
        String insertEvaluationSql = "INSERT INTO evaluation (alternative_id, criterion_id, value) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Save Session
                try (PreparedStatement pstmt = conn.prepareStatement(insertSessionSql)) {
                    pstmt.setString(1, session.getId());
                    pstmt.setString(2, session.getName());
                    pstmt.setTimestamp(3, session.getCreatedAt());
                    pstmt.executeUpdate();
                }

                // Save Criteria
                try (PreparedStatement pstmt = conn.prepareStatement(insertCriterionSql)) {
                    for (Criterion c : session.getCriteria()) {
                        pstmt.setString(1, c.getId());
                        pstmt.setString(2, session.getId());
                        pstmt.setString(3, c.getName());
                        pstmt.setDouble(4, c.getWeight());
                        pstmt.setBoolean(5, c.isMaximize());

                        String funcType = "type1"; // default Usual
                        Double p = null, q = null, s = null;

                        PreferenceFunction pf = c.getPreferenceFunction();
                        if (pf instanceof UsualFunction) {
                            funcType = "type1";
                        } else if (pf instanceof UshapeFunction) {
                            funcType = "type2";
                            q = ((UshapeFunction) pf).getQ();
                        } else if (pf instanceof VShapeFunction) {
                            funcType = "type3";
                            p = ((VShapeFunction) pf).getP();
                        } else if (pf instanceof LevelFunction) {
                            funcType = "type4";
                            q = ((LevelFunction) pf).getQ();
                            p = ((LevelFunction) pf).getP();
                        } else if (pf instanceof VShapeIndifferences) {
                            funcType = "type5";
                            q = ((VShapeIndifferences) pf).getQ();
                            p = ((VShapeIndifferences) pf).getP();
                        } else if (pf instanceof GaussianFunction) {
                            funcType = "type6";
                            s = ((GaussianFunction) pf).getS();
                        }

                        pstmt.setString(6, funcType);
                        if (p != null) pstmt.setDouble(7, p); else pstmt.setNull(7, java.sql.Types.DOUBLE);
                        if (q != null) pstmt.setDouble(8, q); else pstmt.setNull(8, java.sql.Types.DOUBLE);
                        if (s != null) pstmt.setDouble(9, s); else pstmt.setNull(9, java.sql.Types.DOUBLE);

                        pstmt.executeUpdate();
                    }
                }

                // Save Alternatives
                try (PreparedStatement pstmtAlt = conn.prepareStatement(insertAlternativeSql);
                     PreparedStatement pstmtEval = conn.prepareStatement(insertEvaluationSql)) {
                    for (Alternative alt : session.getAlternatives()) {
                        pstmtAlt.setString(1, alt.getId());
                        pstmtAlt.setString(2, session.getId());
                        pstmtAlt.setString(3, alt.getName());
                        pstmtAlt.executeUpdate();

                        // Save Evaluations
                        for (Map.Entry<Criterion, Double> entry : alt.getValues().entrySet()) {
                            pstmtEval.setString(1, alt.getId());
                            pstmtEval.setString(2, entry.getKey().getId());
                            pstmtEval.setDouble(3, entry.getValue());
                            pstmtEval.executeUpdate();
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Retrieves all saved sessions from the database, sorted by creation date descending.
     * Note: This method only loads the session metadata (ID, name, createdAt), not the full details.
     * 
     * @return a list of Session objects containing basic information
     * @throws SQLException if a database access error occurs
     */
    public List<Session> getAllSessions() throws SQLException {
        List<Session> sessions = new ArrayList<>();
        String sql = "SELECT id, name, created_at FROM session ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                sessions.add(new Session(rs.getString("id"), rs.getString("name"), rs.getTimestamp("created_at")));
            }
        }
        return sessions;
    }

    /**
     * Loads a complete session, including its criteria, alternatives, and evaluations, 
     * from the database given its ID.
     * 
     * @param sessionId the unique identifier of the session to load
     * @return the fully reconstructed Session object, or null if no session was found
     * @throws SQLException if a database access error occurs
     */
    public Session loadSession(String sessionId) throws SQLException {
        Session session = null;
        String selectSession = "SELECT * FROM session WHERE id = ?";
        String selectCriteria = "SELECT * FROM criterion WHERE session_id = ?";
        String selectAlternatives = "SELECT * FROM alternative WHERE session_id = ?";
        String selectEvaluations = "SELECT * FROM evaluation WHERE alternative_id = ?";

        try (Connection conn = getConnection()) {
            // Load Session
            try (PreparedStatement pstmt = conn.prepareStatement(selectSession)) {
                pstmt.setString(1, sessionId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        session = new Session(rs.getString("id"), rs.getString("name"), rs.getTimestamp("created_at"));
                    }
                }
            }

            if (session == null) return null;

            // Load Criteria
            Map<String, Criterion> criterionMap = new HashMap<>();
            try (PreparedStatement pstmt = conn.prepareStatement(selectCriteria)) {
                pstmt.setString(1, sessionId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("function_type");
                        double p = rs.getDouble("param_p");
                        double q = rs.getDouble("param_q");
                        double s = rs.getDouble("param_s");

                        PreferenceFunction pf = null;
                        switch (type) {
                            case "type1": pf = new UsualFunction(); break;
                            case "type2": pf = new UshapeFunction(q); break;
                            case "type3": pf = new VShapeFunction(p); break;
                            case "type4": pf = new LevelFunction(q, p); break;
                            case "type5": pf = new VShapeIndifferences(q, p); break;
                            case "type6": pf = new GaussianFunction(s); break;
                        }

                        Criterion criterion = new Criterion(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getDouble("weight"),
                                rs.getBoolean("is_maximize"),
                                pf
                        );
                        session.getCriteria().add(criterion);
                        criterionMap.put(criterion.getId(), criterion);
                    }
                }
            }

            // Load Alternatives and Evaluations
            try (PreparedStatement pstmtAlt = conn.prepareStatement(selectAlternatives);
                 PreparedStatement pstmtEval = conn.prepareStatement(selectEvaluations)) {
                 
                pstmtAlt.setString(1, sessionId);
                try (ResultSet rsAlt = pstmtAlt.executeQuery()) {
                    while (rsAlt.next()) {
                        Alternative alt = new Alternative(rsAlt.getString("id"), rsAlt.getString("name"));
                        
                        pstmtEval.setString(1, alt.getId());
                        try (ResultSet rsEval = pstmtEval.executeQuery()) {
                            while (rsEval.next()) {
                                String critId = rsEval.getString("criterion_id");
                                double value = rsEval.getDouble("value");
                                Criterion c = criterionMap.get(critId);
                                if (c != null) {
                                    alt.addValue(c, value);
                                }
                            }
                        }
                        session.getAlternatives().add(alt);
                    }
                }
            }
        }
        return session;
    }
}