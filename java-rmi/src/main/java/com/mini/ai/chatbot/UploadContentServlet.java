package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Serves uploaded file content by file_id for AI context (GET /api/uploads/{fileId}).
 */
public class UploadContentServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(UploadContentServlet.class);

    private static String getUploadDir() {
        String dir = System.getenv("NOVA_UPLOAD_DIR");
        if (dir != null && !dir.isBlank()) return dir;
        String root = System.getenv("NOVA_PROJECT_ROOT");
        if (root != null && !root.isBlank()) return root + "/uploads";
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize().toString();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        addCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            sendJsonError(resp, 400, "Missing file id");
            return;
        }
        String fileId = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        // fileId from frontend is like "uuid.txt" (after stripping "file_" prefix in JS) or "file_uuid.txt"
        String diskName = fileId.startsWith("file_") ? fileId.substring(5) : fileId;
        
        // Search for file in organized folders
        Path filePath = findFileInUploads(diskName);
        
        if (filePath == null) {
            sendJsonError(resp, 404, "File not found");
            return;
        }

        try {
            // Check if it's a PDF file and extract text using Python AI server
            if (diskName.toLowerCase().endsWith(".pdf")) {
                logger.info("Extracting text from PDF: {}", diskName);
                try {
                    // Call Python AI server to extract PDF text
                    // Get port from environment variable, fallback to default
                    String pythonPort = System.getenv("PYTHON_AI_PORT");
                    if (pythonPort == null || pythonPort.isEmpty()) {
                        pythonPort = "25333";
                    }
                    URL pythonUrl = new URL("http://localhost:" + pythonPort + "/extract_pdf");
                    HttpURLConnection connection = (HttpURLConnection) pythonUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(10000);
                    
                    // Send file path to Python server
                    String jsonRequest = String.format("{\"file_path\":\"%s\"}", filePath.toString().replace("\\", "\\\\"));
                    logger.info("Sending PDF extraction request: {}", jsonRequest);
                    connection.getOutputStream().write(jsonRequest.getBytes(StandardCharsets.UTF_8));
                    
                    // Check response code
                    int responseCode = connection.getResponseCode();
                    logger.info("PDF extraction response code: {}", responseCode);
                    
                    if (responseCode == 200) {
                        // Read the extracted text response
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        
                        String jsonResponse = response.toString();
                        logger.info("PDF extraction response length: {}", jsonResponse.length());
                        logger.info("PDF extraction response preview: {}", jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
                        
                        // Parse JSON response properly
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(jsonResponse);
                            
                            if (jsonNode.has("status") && "success".equals(jsonNode.get("status").asText())) {
                                if (jsonNode.has("content")) {
                                    String extractedText = jsonNode.get("content").asText();
                                    logger.info("Successfully extracted {} characters from PDF", extractedText.length());
                                    String json = String.format("{\"status\":\"success\",\"content\":%s}", escapeJson(extractedText));
                                    resp.getWriter().write(json);
                                    return;
                                }
                            }
                        } catch (Exception parseEx) {
                            logger.warn("Failed to parse PDF extraction JSON: {}", parseEx.getMessage());
                            logger.warn("JSON response was: {}", jsonResponse);
                        }
                    } else {
                        logger.warn("PDF extraction failed with response code: {}", responseCode);
                    }
                    
                    logger.warn("PDF extraction failed, falling back to binary content");
                } catch (Exception e) {
                    logger.error("PDF extraction error: {}", e.getMessage(), e);
                }
            }
            
            // Fallback: return binary content as base64 or limited preview
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            String json = String.format("{\"status\":\"success\",\"content\":%s}",
                escapeJson(content.length() > 1000 ? content.substring(0, 1000) + "...[truncated]" : content));
            resp.getWriter().write(json);
        } catch (IOException e) {
            logger.warn("Could not read file as UTF-8: {}", e.getMessage());
            sendJsonError(resp, 500, "Could not read file content");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private void sendJsonError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        String errorJson = String.format("{\"status\":\"error\",\"message\":\"%s\"}", message.replace("\"", "\\\""));
        resp.getWriter().write(errorJson);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        addCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void addCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
    
    private Path findFileInUploads(String fileName) {
        String uploadDir = getUploadDir();
        
        // Try Images folder first if it's an image
        if (isImageFile(fileName)) {
            Path imagePath = Paths.get(uploadDir, "Images", fileName);
            if (Files.exists(imagePath) && Files.isRegularFile(imagePath)) {
                return imagePath;
            }
        }
        
        // Try Documents folder
        Path docPath = Paths.get(uploadDir, "Documents", fileName);
        if (Files.exists(docPath) && Files.isRegularFile(docPath)) {
            return docPath;
        }
        
        // Fallback to root uploads folder (for backward compatibility)
        Path rootPath = Paths.get(uploadDir, fileName);
        if (Files.exists(rootPath) && Files.isRegularFile(rootPath)) {
            return rootPath;
        }
        
        return null;
    }
    
    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String name = fileName.toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || 
               name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp") || 
               name.endsWith(".svg") || name.endsWith(".ico") || name.endsWith(".tiff");
    }
}
