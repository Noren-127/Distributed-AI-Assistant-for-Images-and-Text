package com.mini.ai.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

/**
 * File Upload Servlet
 */
public class FileUploadServlet extends HttpServlet {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadServlet.class);
    private HadoopManager hadoopManager;

    private static String getUploadDir() {
        String dir = System.getenv("NOVA_UPLOAD_DIR");
        if (dir != null && !dir.isBlank()) return dir;
        String root = System.getenv("NOVA_PROJECT_ROOT");
        if (root != null && !root.isBlank()) return root + "/uploads";
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize().toString();
    }
    
    @Override
    public void init() throws ServletException {
        super.init();
        hadoopManager = new HadoopManager();
        logger.info("FileUploadServlet initialized with HadoopManager");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        addCorsHeaders(resp);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            String sessionId = req.getParameter("sessionId");
            
            if (sessionId == null) {
                sendJsonError(resp, 400, "Missing sessionId parameter");
                return;
            }
            
            String uploadDirPath = getUploadDir();
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // Get file part from request
            Part filePart = req.getPart("file");
            if (filePart == null) {
                sendJsonError(resp, 400, "No file uploaded");
                return;
            }
            
            // Generate unique filename and determine target folder
            String originalFilename = filePart.getSubmittedFileName();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Determine target folder based on file type
            String targetFolder;
            if (isImageFile(fileExtension)) {
                targetFolder = "Images";
            } else if (isDocumentFile(fileExtension)) {
                targetFolder = "Documents";
            } else {
                targetFolder = "Documents"; // Default to Documents for unknown types
            }
            
            String targetDirPath = uploadDirPath + File.separator + targetFolder;
            File targetDir = new File(targetDirPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            
            String filePath = targetDirPath + File.separator + uniqueFilename;
            
            // Save file to disk
            try (InputStream inputStream = filePart.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(filePath)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            // Read file content for AI processing
            String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
            
            // Upload to HDFS using HadoopManager
            String hdfsPath = null;
            try {
                if (hadoopManager != null && hadoopManager.isConnected()) {
                    // Determine HDFS folder based on file type
                    String hdfsFolder;
                    if (isImageFile(fileExtension)) {
                        hdfsFolder = "Images";
                    } else if (isDocumentFile(fileExtension)) {
                        hdfsFolder = "Documents";
                    } else {
                        hdfsFolder = "Documents";
                    }
                    
                    byte[] hdfsFileContent = Files.readAllBytes(Paths.get(filePath));
                    String hdfsStoredPath = hadoopManager.storeFile(sessionId, originalFilename, hdfsFileContent, hdfsFolder);
                    if (hdfsStoredPath != null) {
                        hdfsPath = hdfsStoredPath;
                        logger.info("File uploaded to HDFS: {}", hdfsPath);
                    }
                }
            } catch (Exception hdfsEx) {
                logger.warn("HDFS upload failed, continuing with local storage: {}", hdfsEx.getMessage());
            }
            
            // Store file metadata in session (you could also store in database)
            String fileId = "file_" + uniqueFilename;
            String contentPreview = fileContent.length() > 200 ? fileContent.substring(0, 200) + "..." : fileContent;
            
            // Clean control characters from content preview for JSON safety
            String contentPreviewClean = contentPreview.replaceAll("[\\p{Cntrl}]", " ");
            String filenameClean = (originalFilename != null ? originalFilename : "").replaceAll("[\\p{Cntrl}]", " ");
            String hdfsPathClean = (hdfsPath != null ? hdfsPath : "");
            
            // Use ObjectMapper for proper JSON serialization
            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
                put("status", "success");
                put("file_id", fileId);
                put("filename", filenameClean);
                put("size", new File(filePath).length());
                put("content_preview", contentPreviewClean);
                put("hdfs_path", hdfsPathClean);
                put("message", "File uploaded and ready for AI analysis");
            }});
            
            resp.getWriter().write(jsonResponse);
            logger.info("File uploaded successfully: sessionId={}, filename={}, fileId={}", sessionId, originalFilename, fileId);
            
        } catch (Exception e) {
            logger.error("File upload error: {}", e.getMessage(), e);
            sendJsonError(resp, 500, "Internal server error: " + e.getMessage());
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
    
    private boolean isImageFile(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || 
               ext.equals(".gif") || ext.equals(".bmp") || ext.equals(".webp") || 
               ext.equals(".svg") || ext.equals(".ico") || ext.equals(".tiff");
    }
    
    private boolean isDocumentFile(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".pdf") || ext.equals(".doc") || ext.equals(".docx") || 
               ext.equals(".txt") || ext.equals(".rtf") || ext.equals(".odt") || 
               ext.equals(".xls") || ext.equals(".xlsx") || ext.equals(".ppt") || 
               ext.equals(".pptx") || ext.equals(".csv") || ext.equals(".html") || 
               ext.equals(".htm") || ext.equals(".xml") || ext.equals(".json") || 
               ext.equals(".md") || ext.equals(".log");
    }
}
