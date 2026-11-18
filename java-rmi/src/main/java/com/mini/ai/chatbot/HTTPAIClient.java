package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP client for the Python AI server.
 */
public class HTTPAIClient {
    private static final Logger logger = LoggerFactory.getLogger(HTTPAIClient.class);
    private static final String DEFAULT_PYTHON_PORT = "8400";
    private static final String PYTHON_SERVER_URL;

    static {
        // Get port from environment variable, fallback to default
        String port = System.getenv("PYTHON_AI_PORT");
        if (port == null || port.isEmpty()) {
            port = DEFAULT_PYTHON_PORT;
        }
        PYTHON_SERVER_URL = "http://localhost:" + port;
        logger.info("Python AI Server URL: {}", PYTHON_SERVER_URL);
    }

    private final HttpClient httpClient;

    public HTTPAIClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();
    }

    public String createSession() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PYTHON_SERVER_URL + "/session"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Log the full response for debugging
            logger.info("Python session response status: {}", response.statusCode());
            logger.info("Python session response body: {}", response.body());
            
            return response.body();
        } catch (Exception e) {
            logger.error("Create session failed: {}", e.getMessage());
            return "{\"status\":\"error\"}";
        }
    }

    public String sendMessage(String sessionId, String message) {
        return sendMessage(sessionId, message, null);
    }

    public String sendMessage(String sessionId, String message, String fileContent) {
        return sendMessage(sessionId, message, fileContent, null);
    }

    public String sendMessage(String sessionId, String message, String fileContent, String imageData) {
        try {
            String url = PYTHON_SERVER_URL + "/chat?sessionId=" + URLEncoder.encode(sessionId, StandardCharsets.UTF_8)
                + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            
            // Build request body for large data
            Map<String, String> requestBody = new HashMap<>();
            if (fileContent != null && !fileContent.isBlank()) {
                requestBody.put("fileContent", fileContent);
            }
            if (imageData != null && !imageData.isBlank()) {
                requestBody.put("imageData", imageData);
            }
            
            HttpRequest request;
            if (!requestBody.isEmpty()) {
                // Send data in request body
                ObjectMapper mapper = new ObjectMapper();
                String jsonBody = mapper.writeValueAsString(requestBody);
                request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            } else {
                // No additional data to send
                request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            }
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("Python chat response status: {}", response.statusCode());
            logger.info("Python chat response body: {}", response.body());
            
            return response.body();
        } catch (Exception e) {
            logger.error("Send message failed: {}", e.getMessage());
            String errorMessage = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error";
            return "{\"status\":\"error\",\"message\":\"" + errorMessage + "\"}";
        }
    }

    public String healthCheck() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PYTHON_SERVER_URL + "/health"))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            return "{\"status\":\"unhealthy\"}";
        }
    }

    public String getSessionInfo(String sessionId) {
        try {
            String url = PYTHON_SERVER_URL + "/session?sessionId=" + sessionId;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            logger.error("Get session info failed: {}", e.getMessage());
            return "{\"status\":\"error\"}";
        }
    }
}
