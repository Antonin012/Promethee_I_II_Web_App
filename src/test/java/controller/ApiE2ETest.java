package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End (E2E) Functional Tests for the Application APIs.
 * This class sends real HTTP requests to the deployed application
 * to verify that the full stack (Tomcat, Servlets, Service, DAO, PostgreSQL) works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiE2ETest {

    private static final String BASE_URL = "http://localhost:8080";
    private HttpClient client;
    private ObjectMapper mapper;

    @BeforeAll
    void setup() {
        client = HttpClient.newHttpClient();
        mapper = new ObjectMapper();
    }

    @Test
    void testE2E_CalculateEndpoint_Success() throws Exception {
        // Prepare JSON Payload for /calculate
        ObjectNode dataNode = mapper.createObjectNode();
        dataNode.put("critName_1", "Price");
        dataNode.put("weight_1", "0.6");
        dataNode.put("isMax_1", "false");
        dataNode.put("func_1", "type1");

        dataNode.put("critName_2", "Quality");
        dataNode.put("weight_2", "0.4");
        dataNode.put("isMax_2", "true");
        dataNode.put("func_2", "type1");

        dataNode.put("altName_1", "Car A");
        dataNode.put("val_1_1", "10000"); // Cheaper
        dataNode.put("val_1_2", "5");

        dataNode.put("altName_2", "Car B");
        dataNode.put("val_2_1", "15000");
        dataNode.put("val_2_2", "9");     // Better quality

        // Send POST Request to /calculate
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/calculate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(dataNode.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Verify Response
        assertEquals(200, response.statusCode(), "Server should return 200 OK");
        
        JsonNode responseNode = mapper.readTree(response.body());
        assertTrue(responseNode.has("alternatives"), "Response must contain alternatives array");
        assertTrue(responseNode.has("matrix"), "Response must contain matrix array");

        JsonNode alternatives = responseNode.get("alternatives");
        assertEquals(2, alternatives.size(), "Should return exactly 2 alternatives");
        
        // Assert some calculation results (PROMETHEE logic)
        double phiNetCarA = alternatives.get(0).get("phiNet").asDouble();
        double phiNetCarB = alternatives.get(1).get("phiNet").asDouble();
        
        // Price weight is 0.6, Quality weight is 0.4.
        // Car A wins on Price (+0.6), Car B wins on Quality (+0.4)
        // Therefore Car A should have a higher net flow.
        assertTrue(phiNetCarA > phiNetCarB, "Car A should have a higher Net Flow than Car B");
    }

    @Test
    void testE2E_CalculateEndpoint_EmptyPayload() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/calculate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // We expect it to return an empty array if there are less than 2 alternatives
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void testE2E_DatabasePersistence_SaveAndLoadSession() throws Exception {
        String uniqueSessionName = "E2E Test Session " + UUID.randomUUID().toString().substring(0, 8);

        // Prepare JSON Payload for saving a session
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("sessionName", uniqueSessionName);

        ObjectNode dataNode = mapper.createObjectNode();
        dataNode.put("critName_1", "Speed");
        dataNode.put("weight_1", "1.0");
        dataNode.put("isMax_1", "true");
        dataNode.put("func_1", "type1");

        dataNode.put("altName_1", "Runner 1");
        dataNode.put("val_1_1", "25");

        dataNode.put("altName_2", "Runner 2");
        dataNode.put("val_2_1", "20");

        rootNode.set("data", dataNode);

        // Send POST Request to /api/sessions to SAVE
        HttpRequest saveRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/sessions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(rootNode.toString()))
                .build();

        HttpResponse<String> saveResponse = client.send(saveRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, saveResponse.statusCode(), "Save session should return 200 OK");
        
        JsonNode saveResponseNode = mapper.readTree(saveResponse.body());
        assertEquals("success", saveResponseNode.get("status").asText(), "Status should be success");
        String savedSessionId = saveResponseNode.get("id").asText();
        assertNotNull(savedSessionId);

        // Send GET Request to /api/sessions to LIST ALL
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/sessions"))
                .GET()
                .build();

        HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listResponse.statusCode());
        
        JsonNode listNode = mapper.readTree(listResponse.body());
        assertTrue(listNode.isArray(), "Should return an array of sessions");
        
        boolean foundSession = false;
        for (JsonNode node : listNode) {
            if (node.get("id").asText().equals(savedSessionId)) {
                foundSession = true;
                assertEquals(uniqueSessionName, node.get("name").asText(), "Session name must match");
                break;
            }
        }
        assertTrue(foundSession, "Newly saved session must be present in the list of all sessions");

        // Send GET Request to /api/sessions?id=... to LOAD SPECIFIC SESSION
        HttpRequest loadRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/sessions?id=" + savedSessionId))
                .GET()
                .build();

        HttpResponse<String> loadResponse = client.send(loadRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loadResponse.statusCode());

        JsonNode loadedNode = mapper.readTree(loadResponse.body());
        assertTrue(loadedNode.has("data"), "Loaded session must contain data payload");
        JsonNode loadedData = loadedNode.get("data");
        
        assertEquals("Speed", loadedData.get("critName_1").asText(), "Data integrity check failed for Criterion");
        assertEquals("Runner 1", loadedData.get("altName_1").asText(), "Data integrity check failed for Alternative 1");
        assertEquals("25.0", loadedData.get("val_1_1").asText(), "Data integrity check failed for Value");
    }
}
