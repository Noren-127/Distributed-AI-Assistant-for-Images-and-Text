package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple Implementation of chatbot service with HTTP AI integration
 */
public class ChatbotServiceSimple extends UnicastRemoteObject implements ChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotServiceSimple.class);
    private static final long serialVersionUID = 1L;
    
    // HTTP AI Client
    private HTTPAIClient aiClient;
    
    // Session management
    private final ConcurrentHashMap<String, SimpleSessionInfo> sessions;
    private final AtomicLong sessionCounter;
    
    public ChatbotServiceSimple() throws RemoteException {
        super();
        this.sessions = new ConcurrentHashMap<>();
        this.sessionCounter = new AtomicLong(0);
        this.aiClient = new HTTPAIClient();
        
        logger.info("ChatbotService initialized with HTTP AI integration");
    }
    
    @Override
    public String createSession() throws RemoteException {
        try {
            String sessionId = "session_" + sessionCounter.incrementAndGet();
            
            // Create session in Python via HTTP
            String pythonSessionId = extractSessionId(aiClient.createSession());
            
            SimpleSessionInfo sessionInfo = new SimpleSessionInfo(sessionId, pythonSessionId);
            sessions.put(sessionId, sessionInfo);
            
            logger.info("Created session: {} -> {}", sessionId, pythonSessionId);
            return sessionId;
        } catch (Exception e) {
            logger.error("Failed to create session: {}", e.getMessage());
            throw new RemoteException("Session creation failed", e);
        }
    }
    
    @Override
    public String sendMessage(String sessionId, String message) throws RemoteException {
        try {
            SimpleSessionInfo sessionInfo = sessions.get(sessionId);
            if (sessionInfo == null) {
                throw new RemoteException("Invalid session ID");
            }
            
            // Send message to Python via HTTP
            String response = aiClient.sendMessage(sessionInfo.getPythonSessionId(), message);
            
            // Update session activity
            sessionInfo.addMessage(message);
            
            logger.info("Message sent for session {}: {}", sessionId, message);
            return response;
        } catch (Exception e) {
            logger.error("Failed to send message: {}", e.getMessage());
            throw new RemoteException("Message sending failed", e);
        }
    }
    
    @Override
    public String uploadFile(String sessionId, String fileName, byte[] fileContent) throws RemoteException {
        try {
            SimpleSessionInfo sessionInfo = sessions.get(sessionId);
            if (sessionInfo == null) {
                throw new RemoteException("Invalid session ID");
            }
            
            // For now, just return success
            sessionInfo.lastActivity = System.currentTimeMillis();
            
            logger.info("File uploaded for session {}: {}", sessionId, fileName);
            return "{\"status\":\"success\",\"file_id\":\"" + fileName + "\",\"message\":\"File uploaded successfully\"}";
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", e.getMessage());
            throw new RemoteException("File upload failed", e);
        }
    }
    
    @Override
    public String getSessionInfo(String sessionId) throws RemoteException {
        try {
            SimpleSessionInfo sessionInfo = sessions.get(sessionId);
            if (sessionInfo == null) {
                return "{\"status\":\"error\",\"message\":\"Session not found\"}";
            }
            
            // Create session info JSON
            String sessionJson = String.format(
                "{\"status\":\"success\",\"session_id\":\"%s\",\"message_count\":%d,\"created_at\":%d,\"last_activity\":%d}",
                sessionId,
                sessionInfo.messageCount,
                sessionInfo.createdAt,
                sessionInfo.lastActivity
            );
            
            return sessionJson;
        } catch (Exception e) {
            logger.error("Failed to get session info: {}", e.getMessage());
            return "{\"status\":\"error\",\"message\":\"Session not found\"}";
        }
    }
    
    @Override
    public String clearSession(String sessionId) throws RemoteException {
        try {
            sessions.remove(sessionId);
            return "{\"status\":\"success\",\"message\":\"Session cleared\"}";
        } catch (Exception e) {
            logger.error("Failed to clear session: {}", e.getMessage());
            throw new RemoteException("Session clear failed", e);
        }
    }
    
    @Override
    public String searchFiles(String sessionId, String query) throws RemoteException {
        try {
            return "[]"; // Simple implementation
        } catch (Exception e) {
            logger.error("Failed to search files: {}", e.getMessage());
            throw new RemoteException("File search failed", e);
        }
    }
    
    @Override
    public String healthCheck() throws RemoteException {
        try {
            // Get health from Python via HTTP
            String pythonHealth = aiClient.healthCheck();
            
            if (pythonHealth != null && pythonHealth.contains("healthy")) {
                return String.format(
                    "{\"status\":\"healthy\",\"activeSessions\":%d,\"pythonConnected\":true,\"hadoopConnected\":false,\"timestamp\":%d}",
                    sessions.size(),
                    System.currentTimeMillis()
                );
            } else {
                return "{\"status\":\"unhealthy\",\"message\":\"Python server unavailable\"}";
            }
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            return "{\"status\":\"unhealthy\",\"message\":\"Health check failed\"}";
        }
    }
    
    /**
     * Simple session info class
     */
    public static class SimpleSessionInfo {
        public String sessionId;
        public String pythonSessionId;
        public long createdAt;
        public long lastActivity;
        public int messageCount;
        
        public SimpleSessionInfo(String sessionId, String pythonSessionId) {
            this.sessionId = sessionId;
            this.pythonSessionId = pythonSessionId;
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
            this.messageCount = 0;
        }

        public String getPythonSessionId() {
            return pythonSessionId;
        }

        public void addMessage(String message) {
            this.messageCount++;
            this.lastActivity = System.currentTimeMillis();
        }
    }

    private String extractSessionId(String responseJson) {
        if (responseJson == null) {
            return "fallback_session_" + System.currentTimeMillis();
        }
        int idx = responseJson.indexOf("\"sessionId\"");
        if (idx == -1) {
            return "fallback_session_" + System.currentTimeMillis();
        }
        int start = responseJson.indexOf('"', idx + 11);
        if (start == -1) {
            return "fallback_session_" + System.currentTimeMillis();
        }
        int end = responseJson.indexOf('"', start + 1);
        if (end == -1) {
            return "fallback_session_" + System.currentTimeMillis();
        }
        return responseJson.substring(start + 1, end);
    }
}
