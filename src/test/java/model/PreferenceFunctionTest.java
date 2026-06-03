package model;

import model.function.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreferenceFunctionTest {

    @Test
    void testUsualFunction() {
        PreferenceFunction f = new UsualFunction();
        assertEquals(0.0, f.calculate(-1.0));
        assertEquals(0.0, f.calculate(0.0));
        assertEquals(1.0, f.calculate(1.0));
    }

    @Test
    void testUshapeFunction() {
        PreferenceFunction f = new UshapeFunction(10.0);
        assertEquals(0.0, f.calculate(5.0));
        assertEquals(0.0, f.calculate(10.0));
        assertEquals(1.0, f.calculate(10.1));
    }

    @Test
    void testVShapeFunction() {
        PreferenceFunction f = new VShapeFunction(10.0);
        assertEquals(0.0, f.calculate(0.0));
        assertEquals(0.5, f.calculate(5.0));
        assertEquals(1.0, f.calculate(10.0));
        assertEquals(1.0, f.calculate(15.0));
    }

    @Test
    void testLevelFunction() {
        PreferenceFunction f = new LevelFunction(5.0, 10.0);
        assertEquals(0.0, f.calculate(4.0));
        assertEquals(0.0, f.calculate(5.0));
        assertEquals(0.5, f.calculate(7.0));
        assertEquals(0.5, f.calculate(10.0));
        assertEquals(1.0, f.calculate(11.0));
    }

    @Test
    void testVShapeIndifferences() {
        PreferenceFunction f = new VShapeIndifferences(5.0, 15.0);
        assertEquals(0.0, f.calculate(5.0));
        assertEquals(0.5, f.calculate(10.0)); // (10-5)/(15-5) = 5/10 = 0.5
        assertEquals(1.0, f.calculate(15.0));
    }

    @Test
    void testGaussianFunction() {
        PreferenceFunction f = new GaussianFunction(1.0);
        assertEquals(0.0, f.calculate(0.0));
        assertTrue(f.calculate(1.0) > 0);
        assertTrue(f.calculate(10.0) < 1.0001 && f.calculate(10.0) > 0.99);
    }
}
