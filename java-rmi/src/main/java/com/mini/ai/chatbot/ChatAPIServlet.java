package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Chat API Servlet
 */
public class ChatAPIServlet extends HttpServlet {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatAPIServlet.class);
    private final HTTPAIClient aiClient = new HTTPAIClient();
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        addCorsHeaders(resp);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            String sessionId = req.getParameter("sessionId");
            String message = req.getParameter("message");
            String fileContent = null;
            String imageData = null;
            
            // Try to read file content and image data from JSON request body
            if (req.getContentType() != null && req.getContentType().contains("application/json")) {
                try {
                    BufferedReader reader = req.getReader();
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    
                    String jsonBody = sb.toString();
                    
                    // Only parse if body is not empty
                    if (!jsonBody.trim().isEmpty()) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(jsonBody);
                        if (jsonNode.has("fileContent") && !jsonNode.get("fileContent").isNull()) {
                            fileContent = jsonNode.get("fileContent").asText();
                        }
                        if (jsonNode.has("imageData") && !jsonNode.get("imageData").isNull()) {
                            imageData = jsonNode.get("imageData").asText();
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Could not parse JSON request body: {}", e.getMessage());
                }
            }
            
            if (sessionId == null || message == null) {
                sendJsonError(resp, 400, "Missing sessionId or message parameter");
                return;
            }
            
            String aiResponse = aiClient.sendMessage(sessionId, message, fileContent, imageData);
            if (aiResponse == null || aiResponse.isBlank()) {
                aiResponse = String.format(
                    "{\"status\":\"success\",\"response\":\"Sorry, no response received.\",\"timestamp\":%d}",
                    System.currentTimeMillis()
                );
            }
            resp.getWriter().write(aiResponse);
            logger.info("Chat API proxied: sessionId={}, message={}, hasFileContent={}, hasImageData={}", sessionId, message, fileContent != null, imageData != null);
            
        } catch (Exception e) {
            logger.error("Chat API error: {}", e.getMessage());
            sendJsonError(resp, 500, "Internal server error");
        }
    }
    
    private void sendJsonError(HttpServletResponse resp, int code, String message) throws IOException {
        addCorsHeaders(resp);
        resp.setStatus(code);
        String errorJson = String.format(
            "{\"status\":\"error\",\"code\":%d,\"message\":\"%s\"}",
            code, message
        );
        resp.getWriter().write(errorJson);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        addCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void addCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
