package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Alternative;
import model.CalculationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the PrometheeService.
 * This test validates the integration between the JSON parsing (Jackson),
 * the Service layer orchestration, the object Models, and the Calculation engine.
 */
class PrometheeServiceIntegrationTest {

    private PrometheeService prometheeService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        prometheeService = new PrometheeService();
        mapper = new ObjectMapper();
    }

    @Test
    void testProcessFullCalculation_Integration() {
        // 1. Create a simulated JSON payload from the frontend
        ObjectNode rootNode = mapper.createObjectNode();
        
        // Setup Criteria
        rootNode.put("critName_1", "Price");
        rootNode.put("weight_1", "0.5");
        rootNode.put("isMax_1", "false"); // Minimize
        rootNode.put("func_1", "type3"); // V-Shape
        rootNode.put("p_1", "2000");

        rootNode.put("critName_2", "Quality");
        rootNode.put("weight_2", "0.5");
        rootNode.put("isMax_2", "true"); // Maximize
        rootNode.put("func_1", "type1"); // Usual

        // Setup Alternatives
        rootNode.put("altName_1", "Car A");
        rootNode.put("val_1_1", "10000"); // Price
        rootNode.put("val_1_2", "8");     // Quality

        rootNode.put("altName_2", "Car B");
        rootNode.put("val_2_1", "12000"); // Price
        rootNode.put("val_2_2", "9");     // Quality
        
        rootNode.put("altName_3", "Car C");
        rootNode.put("val_3_1", "15000"); // Price
        rootNode.put("val_3_2", "7");     // Quality

        // 2. Execute the Service (Integration Point)
        CalculationResult result = prometheeService.processFullCalculation(rootNode);

        // 3. Verify the Integration Results
        assertNotNull(result);
        
        List<Alternative> alternatives = result.getAlternatives();
        double[][] matrix = result.getMatrix();

        assertEquals(3, alternatives.size(), "Should have extracted 3 alternatives");
        assertEquals(3, matrix.length, "Matrix should be 3x3");

        // Verify that the alternatives were mapped and calculated correctly
        Alternative carA = alternatives.get(0);
        Alternative carB = alternatives.get(1);
        Alternative carC = alternatives.get(2);

        assertEquals("Car A", carA.getName());
        assertEquals("Car B", carB.getName());
        assertEquals("Car C", carC.getName());

        // Car A is best on Price but 2nd on Quality. 
        // Car C is worst on both.
        // We ensure that PhiNet calculations executed successfully across all layers.
        assertNotNull(carA.getPhiNet());
        assertNotNull(carB.getPhiNet());
        assertNotNull(carC.getPhiNet());
        
        // Assert Car A vs Car C preference logic went through
        // A is cheaper by 5000 (p=2000), so Pref(A,C) for Price = 1.0. Weight = 0.5.
        assertTrue(carA.getPhiNet() > carC.getPhiNet(), "Car A should be strictly better than Car C");
    }
}
