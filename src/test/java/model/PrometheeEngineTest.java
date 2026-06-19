package model;

import model.function.*;
import service.PrometheeEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PrometheeEngineTest {

    private PrometheeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PrometheeEngine();
    }

    @Test
    void testCalculateMatrix_WithUsualFunction_Minimize() {
        List<Criterion> criteria = new ArrayList<>();
        Criterion price = new Criterion("Price", 1.0, false, new UsualFunction());
        criteria.add(price);

        List<Alternative> alternatives = new ArrayList<>();
        Alternative a1 = new Alternative("1", "Car A");
        a1.addValue(price, 10000.0);
        Alternative a2 = new Alternative("2", "Car B");
        a2.addValue(price, 15000.0);
        alternatives.add(a1);
        alternatives.add(a2);

        double[][] matrix = engine.calculate(alternatives, criteria);

        assertNotNull(matrix);
        assertEquals(2, matrix.length);
        
        // Car A is better (lower price), so P(A,B) = 1.0
        // diff = ValB - ValA = 15000 - 10000 = 5000 > 0 -> Pref=1
        assertEquals(1.0, matrix[0][1], 0.001);
        assertEquals(0.0, matrix[1][0], 0.001);
    }

    @Test
    void testCalculateMatrix_WithVShapeFunction_Maximize() {
        List<Criterion> criteria = new ArrayList<>();
        // Quality: Maximize, V-Shape with p=10
        Criterion quality = new Criterion("Quality", 1.0, true, new VShapeFunction(10.0));
        criteria.add(quality);

        List<Alternative> alternatives = new ArrayList<>();
        Alternative a1 = new Alternative("1", "Product A");
        a1.addValue(quality, 80.0);
        Alternative a2 = new Alternative("2", "Product B");
        a2.addValue(quality, 85.0);
        Alternative a3 = new Alternative("3", "Product C");
        a3.addValue(quality, 70.0);
        alternatives.add(a1);
        alternatives.add(a2);
        alternatives.add(a3);

        double[][] matrix = engine.calculate(alternatives, criteria);

        assertNotNull(matrix);
        assertEquals(3, matrix.length);
        
        // P(B, A): diff = 85 - 80 = 5. V-Shape(p=10) for 5 is 5/10 = 0.5
        assertEquals(0.5, matrix[1][0], 0.001);
        
        // P(A, B): diff = 80 - 85 = -5. V-Shape is 0
        assertEquals(0.0, matrix[0][1], 0.001);

        // P(B, C): diff = 85 - 70 = 15. V-Shape(p=10) for 15 is 1.0 (since 15 > p)
        assertEquals(1.0, matrix[1][2], 0.001);
    }

    @Test
    void testFlowsAndRanking_MultipleCriteria() {
        List<Criterion> criteria = new ArrayList<>();
        Criterion c1 = new Criterion("C1", 0.6, true, new UsualFunction());
        Criterion c2 = new Criterion("C2", 0.4, false, new UsualFunction());
        criteria.add(c1);
        criteria.add(c2);

        List<Alternative> alternatives = new ArrayList<>();
        Alternative a1 = new Alternative("1", "A1");
        a1.addValue(c1, 10.0);
        a1.addValue(c2, 5.0);
        
        Alternative a2 = new Alternative("2", "A2");
        a2.addValue(c1, 8.0);
        a2.addValue(c2, 2.0);

        Alternative a3 = new Alternative("3", "A3");
        a3.addValue(c1, 12.0);
        a3.addValue(c2, 10.0);

        alternatives.add(a1);
        alternatives.add(a2);
        alternatives.add(a3);

        engine.calculate(alternatives, criteria);

        // Verify Phi flows calculations
        // C1 (Max): A3 > A1 > A2
        // C2 (Min): A2 > A1 > A3

        // Pref Matrix:
        //        A1     A2     A3
        // A1     0      0.6    0.4  (Sum = 1.0, Phi+ = 0.5)
        // A2     0.4    0      0.4  (Sum = 0.8, Phi+ = 0.4)
        // A3     0.6    0.6    0    (Sum = 1.2, Phi+ = 0.6)

        assertEquals(0.5, a1.getPhiPlus(), 0.001);
        assertEquals(0.4, a2.getPhiPlus(), 0.001);
        assertEquals(0.6, a3.getPhiPlus(), 0.001);

        assertEquals(0.5, a1.getPhiMinus(), 0.001);
        assertEquals(0.6, a2.getPhiMinus(), 0.001);
        assertEquals(0.4, a3.getPhiMinus(), 0.001);

        assertEquals(0.0, a1.getPhiNet(), 0.001);
        assertEquals(-0.2, a2.getPhiNet(), 0.001);
        assertEquals(0.2, a3.getPhiNet(), 0.001);
    }

    @Test
    void testEmptyData() {
        double[][] matrix = engine.calculate(new ArrayList<>(), new ArrayList<>());
        assertEquals(0, matrix.length);
    }

    @Test
    void testSingleAlternative() {
        List<Alternative> alternatives = new ArrayList<>();
        alternatives.add(new Alternative("1", "A1"));
        
        List<Criterion> criteria = new ArrayList<>();
        criteria.add(new Criterion("C1", 1.0, true, new UsualFunction()));

        double[][] matrix = engine.calculate(alternatives, criteria);
        assertEquals(0, matrix.length);
    }

    @Test
    void testNullValues() {
        double[][] matrix = engine.calculate(null, null);
        assertEquals(0, matrix.length);
    }
}
