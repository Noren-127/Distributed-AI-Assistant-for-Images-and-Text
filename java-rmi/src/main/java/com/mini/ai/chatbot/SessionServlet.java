package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Session Servlet
 */
public class SessionServlet extends HttpServlet {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionServlet.class);
    private final HTTPAIClient aiClient = new HTTPAIClient();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        addCorsHeaders(resp);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        handleSessionRequest(req, resp, true);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        addCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        handleSessionRequest(req, resp, false);
    }

    private void handleSessionRequest(HttpServletRequest req, HttpServletResponse resp, boolean allowInfo) throws IOException {
        try {
            String sessionId = req.getParameter("sessionId");

            if (sessionId == null || !allowInfo) {
                String responseJson = aiClient.createSession();
                resp.getWriter().write(responseJson);
                logger.info("Created Python session");
                return;
            }

            String responseJson = aiClient.getSessionInfo(sessionId);
            resp.getWriter().write(responseJson);
            logger.info("Retrieved Python session info: {}", sessionId);
        } catch (Exception e) {
            logger.error("Session API error: {}", e.getMessage());
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
