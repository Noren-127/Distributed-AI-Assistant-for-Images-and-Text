package com.mini.ai.chatbot;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Remote interface for the chatbot service
 */
public interface ChatbotService extends Remote {
    
    /**
     * Create a new chat session
     * @return session ID
     */
    String createSession() throws RemoteException;
    
    /**
     * Send a message to the AI
     * @param sessionId the session ID
     * @param message the user message
     * @return AI response
     */
    String sendMessage(String sessionId, String message) throws RemoteException;
    
    /**
     * Upload a file for processing
     * @param sessionId the session ID
     * @param fileName the file name
     * @param fileContent the file content as byte array
     * @return upload result
     */
    String uploadFile(String sessionId, String fileName, byte[] fileContent) throws RemoteException;
    
    /**
     * Get session information
     * @param sessionId the session ID
     * @return session info as JSON
     */
    String getSessionInfo(String sessionId) throws RemoteException;
    
    /**
     * Clear session history
     * @param sessionId the session ID
     * @return operation result
     */
    String clearSession(String sessionId) throws RemoteException;
    
    /**
     * Search through uploaded files
     * @param sessionId the session ID
     * @param query search query
     * @return search results as JSON
     */
    String searchFiles(String sessionId, String query) throws RemoteException;
    
    /**
     * Health check
     * @return service status
     */
    String healthCheck() throws RemoteException;
}
