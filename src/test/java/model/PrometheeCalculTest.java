package model;

import model.function.UsualFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PrometheeCalculTest {

    private PrometheeCalcul engine;
    private List<Criterion> criteria;
    private List<Alternative> alternatives;

    @BeforeEach
    void setUp() {
        engine = new PrometheeCalcul();
        criteria = new ArrayList<>();
        alternatives = new ArrayList<>();

        // One criterion: Price, Weight=1.0, Minimize, Usual Function
        Criterion price = new Criterion("Price", 1.0, false, new UsualFunction());
        criteria.add(price);

        // Two alternatives
        Alternative a1 = new Alternative("1", "Car A");
        a1.addValue(price, 10000.0);
        
        Alternative a2 = new Alternative("2", "Car B");
        a2.addValue(price, 15000.0);

        alternatives.add(a1);
        alternatives.add(a2);
    }

    @Test
    void testCalculateMatrix() {
        double[][] matrix = engine.calculate(alternatives, criteria);

        assertNotNull(matrix);
        assertEquals(2, matrix.length);
        
        // Car A is better (lower price), so P(A,B) = 1.0
        // diff = ValB - ValA = 15000 - 10000 = 5000 > 0 -> Pref=1
        assertEquals(1.0, matrix[0][1], 0.001);
        assertEquals(0.0, matrix[1][0], 0.001);
    }

    @Test
    void testFlowsAndRanking() {
        engine.calculate(alternatives, criteria);

        Alternative a1 = alternatives.get(0);
        Alternative a2 = alternatives.get(1);

        // A1: Phi+ = 1.0, Phi- = 0.0, PhiNet = 1.0
        assertEquals(1.0, a1.getPhiPlus(), 0.001);
        assertEquals(0.0, a1.getPhiMinus(), 0.001);
        assertEquals(1.0, a1.getPhiNet(), 0.001);

        // A2: Phi+ = 0.0, Phi- = 1.0, PhiNet = -1.0
        assertEquals(0.0, a2.getPhiPlus(), 0.001);
        assertEquals(1.0, a2.getPhiMinus(), 0.001);
        assertEquals(-1.0, a2.getPhiNet(), 0.001);
    }

    @Test
    void testEmptyData() {
        double[][] matrix = engine.calculate(new ArrayList<>(), new ArrayList<>());
        assertEquals(0, matrix.length);
    }
}
