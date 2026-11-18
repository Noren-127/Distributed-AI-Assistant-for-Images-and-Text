package com.mini.ai.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private HadoopManager hadoopManager;
    
    private String getChatHistoryDir() {
        String uploadDir = System.getenv("NOVA_UPLOAD_DIR");
        if (uploadDir != null && !uploadDir.isBlank()) {
            return uploadDir + File.separator + "ChatHistory";
        }
        String root = System.getenv("NOVA_PROJECT_ROOT");
        if (root != null && !root.isBlank()) {
            return root + File.separator + "uploads" + File.separator + "ChatHistory";
        }
        return Paths.get(System.getProperty("user.dir"), "uploads", "ChatHistory").toAbsolutePath().normalize().toString();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        hadoopManager = new HadoopManager();
        // Create chat history directory if it doesn't exist
        try {
            String historyDir = getChatHistoryDir();
            Files.createDirectories(Paths.get(historyDir));
            logger.info("Chat history directory initialized: {}", historyDir);
        } catch (IOException e) {
            logger.error("Failed to create chat history directory: {}", e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        
        String action = req.getParameter("action");
        String chatId = req.getParameter("chatId");
        
        try {
            if ("delete".equals(action) && chatId != null) {
                deleteChatHistory(chatId);
                sendJsonResponse(resp, "{\"status\":\"success\",\"message\":\"Chat deleted successfully\"}");
            } else if ("clear".equals(action)) {
                clearAllChatHistory();
                sendJsonResponse(resp, "{\"status\":\"success\",\"message\":\"All chat history cleared\"}");
            } else {
                // Get all chat history
                List<ChatHistory> history = loadAllChatHistory();
                String json = mapper.writeValueAsString(history);
                sendJsonResponse(resp, json);
            }
        } catch (Exception e) {
            logger.error("Error in chat history servlet: {}", e.getMessage(), e);
            sendJsonResponse(resp, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}", 500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        
        try {
            JsonNode requestBody = mapper.readTree(req.getReader());
            String chatId = requestBody.get("chatId").asText();
            String title = requestBody.get("title").asText();
            String messages = requestBody.get("messages").toString();
            String timestamp = requestBody.get("timestamp").asText();
            int messageCount = requestBody.get("messageCount").asInt();
            
            saveChatHistory(chatId, title, messages, timestamp, messageCount);
            sendJsonResponse(resp, "{\"status\":\"success\",\"message\":\"Chat saved successfully\"}");
        } catch (Exception e) {
            logger.error("Error saving chat history: {}", e.getMessage(), e);
            sendJsonResponse(resp, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}", 500);
        }
    }

    private void saveChatHistory(String chatId, String title, String messages, String timestamp, int messageCount) throws IOException {
        // Save to local storage
        Path historyFile = Paths.get(getChatHistoryDir(), chatId + ".json");
        ChatHistory chat = new ChatHistory(chatId, title, messages, timestamp, messageCount);
        String json = mapper.writeValueAsString(chat);
        Files.write(historyFile, json.getBytes());
        
        // Also save to HDFS if available
        try {
            if (hadoopManager != null && hadoopManager.isConnected()) {
                boolean hdfsSaved = hadoopManager.saveChatHistory(chatId, title, messages, timestamp, messageCount);
                if (hdfsSaved) {
                    logger.info("Chat history saved to both local and HDFS for session: {}", chatId);
                } else {
                    logger.warn("Failed to save chat history to HDFS for session: {}", chatId);
                }
            }
        } catch (Exception hdfsEx) {
            logger.warn("HDFS save failed, continuing with local storage: {}", hdfsEx.getMessage());
        }
        
        logger.info("Saved chat history for session: {}", chatId);
    }

    private List<ChatHistory> loadAllChatHistory() throws IOException {
        List<ChatHistory> history = new ArrayList<>();
        File historyDir = new File(getChatHistoryDir());
        
        if (historyDir.exists() && historyDir.isDirectory()) {
            File[] files = historyDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        ChatHistory chat = mapper.readValue(content, ChatHistory.class);
                        history.add(chat);
                    } catch (Exception e) {
                        logger.warn("Failed to load chat history file {}: {}", file.getName(), e.getMessage());
                    }
                }
            }
        }
        
        // Sort by timestamp (newest first)
        history.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return history;
    }

    private void deleteChatHistory(String chatId) throws IOException {
        // Delete from local filesystem
        Path historyFile = Paths.get(getChatHistoryDir(), chatId + ".json");
        Files.deleteIfExists(historyFile);
        logger.info("Deleted chat history from local filesystem for session: {}", chatId);
        
        // Delete from HDFS
        if (hadoopManager != null && hadoopManager.isConnected()) {
            boolean hdfsDeleted = hadoopManager.deleteChatHistory(chatId);
            if (hdfsDeleted) {
                logger.info("Deleted chat history from HDFS for session: {}", chatId);
            } else {
                logger.warn("Failed to delete chat history from HDFS for session: {}", chatId);
            }
        } else {
            logger.warn("HadoopManager not available, skipping HDFS deletion for session: {}", chatId);
        }
    }

    private void clearAllChatHistory() throws IOException {
        // Clear from local filesystem
        File historyDir = new File(getChatHistoryDir());
        if (historyDir.exists() && historyDir.isDirectory()) {
            File[] files = historyDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    Files.deleteIfExists(file.toPath());
                }
            }
        }
        logger.info("Cleared all chat history from local filesystem");
        
        // Clear from HDFS
        if (hadoopManager != null && hadoopManager.isConnected()) {
            boolean hdfsCleared = hadoopManager.clearAllChatHistory();
            if (hdfsCleared) {
                logger.info("Cleared all chat history from HDFS");
            } else {
                logger.warn("Failed to clear all chat history from HDFS");
            }
        } else {
            logger.warn("HadoopManager not available, skipping HDFS clear all");
        }
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void sendJsonResponse(HttpServletResponse resp, String json) throws IOException {
        sendJsonResponse(resp, json, 200);
    }

    private void sendJsonResponse(HttpServletResponse resp, String json, int status) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.getWriter().write(json);
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // Inner class to represent chat history
    public static class ChatHistory {
        private String id;
        private String title;
        private String messages;
        private String timestamp;
        private int messageCount;

        public ChatHistory() {}

        public ChatHistory(String id, String title, String messages, String timestamp, int messageCount) {
            this.id = id;
            this.title = title;
            this.messages = messages;
            this.timestamp = timestamp;
            this.messageCount = messageCount;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessages() { return messages; }
        public void setMessages(String messages) { this.messages = messages; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    }
}
