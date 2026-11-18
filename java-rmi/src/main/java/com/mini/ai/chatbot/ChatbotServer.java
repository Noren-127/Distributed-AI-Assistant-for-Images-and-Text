package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import javax.servlet.MultipartConfigElement;
import java.net.InetSocketAddress;

/**
 * Main server class that combines RMI and HTTP services
 */
public class ChatbotServer {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotServer.class);
    private static final int RMI_PORT = 1099;
    private static final int HTTP_PORT;
    
    static {
        // Get HTTP port from environment variable, fallback to default
        String port = System.getenv("JAVA_PORT");
        HTTP_PORT = (port != null && !port.isEmpty()) ? Integer.parseInt(port) : 8082;
        logger.info("Java HTTP Server will use port: {}", HTTP_PORT);
    }
    
    private ChatbotServiceSimple chatbotService;
    private Server httpServer;
    
    public static void main(String[] args) {
        ChatbotServer server = new ChatbotServer();
        server.start();
    }
    
    public void start() {
        logger.info("Starting Mini AI Chatbot Server...");
        
        try {
            initializeRMI();
            initializeHTTP();
            
            logger.info("🎉 Mini AI Chatbot started successfully!");
            logger.info("📍 Web UI: http://localhost:8081");
            logger.info("📍 API: http://localhost:8082");
            logger.info("📍 RMI: port 1099");
            
        } catch (Exception e) {
            logger.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }
    
    private void initializeRMI() throws Exception {
        logger.info("Initializing RMI service...");
        
        chatbotService = new ChatbotServiceSimple();
        
        LocateRegistry.createRegistry(RMI_PORT);
        Naming.rebind("rmi://localhost:" + RMI_PORT + "/ChatbotService", chatbotService);
        
        logger.info("RMI service bound successfully");
    }
    
    private void initializeHTTP() throws Exception {
        logger.info("Initializing HTTP server...");
        
        // Bind to all network interfaces (0.0.0.0) instead of just localhost
        httpServer = new Server(new InetSocketAddress("0.0.0.0", HTTP_PORT));
        
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        httpServer.setHandler(context);
        
        // Add API servlets with CORS
        String uploadDir = System.getenv("NOVA_UPLOAD_DIR");
        if (uploadDir == null || uploadDir.isBlank()) {
            String root = System.getenv("NOVA_PROJECT_ROOT");
            uploadDir = (root != null && !root.isBlank()) ? root + "/uploads" : "uploads";
        }
        String uploadDirAbs = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        ServletHolder uploadServlet = new ServletHolder(new FileUploadServlet());
        uploadServlet.getRegistration().setMultipartConfig(
            new MultipartConfigElement(uploadDirAbs)
        );
        
        context.addServlet(new ServletHolder(new ChatAPIServlet()), "/api/chat/*");
        context.addServlet(uploadServlet, "/api/upload");
        context.addServlet(new ServletHolder(new UploadContentServlet()), "/api/uploads/*");
        context.addServlet(new ServletHolder(new SessionServlet()), "/api/session");
        context.addServlet(new ServletHolder(new ChatHistoryServlet()), "/api/history");
        context.addServlet(new ServletHolder(new HealthServlet()), "/api/health");
        
        httpServer.start();
        logger.info("HTTP server started successfully");
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        try {
            logger.info("Shutting down ChatbotService...");
            if (chatbotService != null) {
                // Note: Simple implementation doesn't have shutdown method
                logger.info("ChatbotService shutdown complete");
            }
        } catch (Exception e) {
            logger.error("Error during shutdown: {}", e.getMessage());
        }
    }
}
