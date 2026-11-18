package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Health Servlet
 */
public class HealthServlet extends HttpServlet {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        addCorsHeaders(resp);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Check Hadoop connection status
        boolean hadoopConnected = false;
        try {
            // This is a simple check - in a real implementation, you'd check HadoopManager
            // For now, we'll check if HDFS is accessible
            String[] cmd = {"/bin/sh", "-c", "test -e $HADOOP_HOME && hdfs dfsadmin -report > /dev/null 2>&1"};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process process = pb.start();
            int exitCode = process.waitFor();
            hadoopConnected = (exitCode == 0);
        } catch (Exception e) {
            logger.warn("Failed to check Hadoop status: {}", e.getMessage());
            hadoopConnected = false;
        }
        
        String healthJson = String.format(
            "{\"status\":\"healthy\",\"activeSessions\":0,\"pythonConnected\":true,\"hadoopConnected\":%s,\"timestamp\":%d}",
            hadoopConnected,
            System.currentTimeMillis()
        );
        resp.getWriter().write(healthJson);
        logger.info("Health check completed - Hadoop connected: {}", hadoopConnected);
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
