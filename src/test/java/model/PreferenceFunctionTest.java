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
        assertEquals(1.0, f.calculate(100.0));
    }

    @Test
    void testUshapeFunction() {
        PreferenceFunction f = new UshapeFunction(10.0);
        assertEquals(0.0, f.calculate(-5.0));
        assertEquals(0.0, f.calculate(0.0));
        assertEquals(0.0, f.calculate(5.0));
        assertEquals(0.0, f.calculate(10.0));
        assertEquals(1.0, f.calculate(10.1));
        assertEquals(1.0, f.calculate(50.0));
        
        assertThrows(IllegalArgumentException.class, () -> new UshapeFunction(-1.0));
    }

    @Test
    void testVShapeFunction() {
        PreferenceFunction f = new VShapeFunction(10.0);
        assertEquals(0.0, f.calculate(-5.0));
        assertEquals(0.0, f.calculate(0.0));
        assertEquals(0.5, f.calculate(5.0));
        assertEquals(1.0, f.calculate(10.0));
        assertEquals(1.0, f.calculate(15.0));
        
        assertThrows(IllegalArgumentException.class, () -> new VShapeFunction(0.0));
        assertThrows(IllegalArgumentException.class, () -> new VShapeFunction(-5.0));
    }

    @Test
    void testLevelFunction() {
        PreferenceFunction f = new LevelFunction(5.0, 10.0);
        assertEquals(0.0, f.calculate(-2.0));
        assertEquals(0.0, f.calculate(0.0));
        assertEquals(0.0, f.calculate(4.0));
        assertEquals(0.0, f.calculate(5.0));
        assertEquals(0.5, f.calculate(5.1));
        assertEquals(0.5, f.calculate(7.0));
        assertEquals(0.5, f.calculate(10.0));
        assertEquals(1.0, f.calculate(10.1));
        assertEquals(1.0, f.calculate(15.0));

        assertThrows(IllegalArgumentException.class, () -> new LevelFunction(-1.0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> new LevelFunction(10.0, 5.0));
        assertThrows(IllegalArgumentException.class, () -> new LevelFunction(10.0, 10.0));
    }

    @Test
    void testVShapeIndifferences() {
        PreferenceFunction f = new VShapeIndifferences(5.0, 15.0);
        assertEquals(0.0, f.calculate(-5.0));
        assertEquals(0.0, f.calculate(0.0));
        assertEquals(0.0, f.calculate(5.0));
        assertEquals(0.1, f.calculate(6.0), 0.0001); // (6-5)/(15-5) = 1/10 = 0.1
        assertEquals(0.5, f.calculate(10.0), 0.0001); // (10-5)/(15-5) = 5/10 = 0.5
        assertEquals(1.0, f.calculate(15.0));
        assertEquals(1.0, f.calculate(20.0));

        assertThrows(IllegalArgumentException.class, () -> new VShapeIndifferences(-1.0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> new VShapeIndifferences(10.0, 5.0));
        assertThrows(IllegalArgumentException.class, () -> new VShapeIndifferences(10.0, 10.0));
    }

    @Test
    void testGaussianFunction() {
        PreferenceFunction f = new GaussianFunction(2.0);
        assertEquals(0.0, f.calculate(-5.0));
        assertEquals(0.0, f.calculate(0.0));
        // Formula: 1 - exp(-x^2 / (2 * s^2)) -> 1 - exp(-1 / 8) -> 1 - exp(-0.125) ~ 0.1175
        assertEquals(0.1175, f.calculate(1.0), 0.001);
        assertEquals(0.3934, f.calculate(2.0), 0.001);
        assertEquals(1.0, f.calculate(100.0), 0.0001);

        assertThrows(IllegalArgumentException.class, () -> new GaussianFunction(0.0));
        assertThrows(IllegalArgumentException.class, () -> new GaussianFunction(-2.0));
    }
}
