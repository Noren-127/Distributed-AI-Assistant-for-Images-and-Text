package com.mini.ai.chatbot;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Hadoop HDFS manager for file storage and chat history
 */
public class HadoopManager {
    
    private static final Logger logger = LoggerFactory.getLogger(HadoopManager.class);
    private static final String HDFS_URI = "hdfs://localhost:9000";
    private static final String CHATBOT_BASE_PATH = "/chatbot";
    
    private FileSystem fs;
    private Configuration conf;
    private boolean connected;
    
    public HadoopManager() {
        initializeHadoop();
    }
    
    private void initializeHadoop() {
        try {
            conf = new Configuration();
            conf.set("fs.defaultFS", HDFS_URI);
            conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
            conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
            
            // Try to connect to HDFS
            try {
                fs = FileSystem.get(URI.create(HDFS_URI), conf);
                
                // Create base directories
                createDirectories();
                connected = true;
                logger.info("Connected to Hadoop HDFS successfully");
                
            } catch (Exception e) {
                logger.warn("HDFS not available, using local filesystem: {}", e.getMessage());
                // Fallback to local filesystem
                fs = FileSystem.getLocal(conf);
                createLocalDirectories();
                connected = false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize Hadoop: {}", e.getMessage());
            connected = false;
        }
    }
    
    private void createDirectories() throws IOException {
        String[] paths = {
            CHATBOT_BASE_PATH,
            CHATBOT_BASE_PATH + "/sessions",
            CHATBOT_BASE_PATH + "/Documents",
            CHATBOT_BASE_PATH + "/Images", 
            CHATBOT_BASE_PATH + "/ChatHistory",
            CHATBOT_BASE_PATH + "/logs"
        };
        
        for (String path : paths) {
            Path dirPath = new Path(path);
            if (!fs.exists(dirPath)) {
                fs.mkdirs(dirPath);
                logger.info("Created directory: {}", path);
            }
        }
    }
    
    private void createLocalDirectories() throws IOException {
        String baseDir = System.getProperty("user.home") + "/chatbot-data";
        String[] paths = {
            baseDir,
            baseDir + "/sessions",
            baseDir + "/files",
            baseDir + "/logs"
        };
        
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
                logger.info("Created local directory: {}", path);
            }
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void storeSession(String sessionId, Object sessionInfo) {
        try {
            String sessionPath = getSessionPath(sessionId);
            Path sessionFile = new Path(sessionPath);
            
            // Serialize session info as JSON
            String sessionJson = String.format(
                "{\"sessionId\":\"%s\",\"createdAt\":%d,\"timestamp\":\"%s\"}",
                sessionId, System.currentTimeMillis(), new Date()
            );
            
            try (FSDataOutputStream out = fs.create(sessionFile, true)) {
                out.write(sessionJson.getBytes(StandardCharsets.UTF_8));
            }
            
            logger.debug("Stored session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to store session: {}", e.getMessage());
        }
    }
    
    public void logMessage(String sessionId, String role, String message) {
        try {
            String logPath = getLogPath(sessionId);
            Path logFile = new Path(logPath);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            String logEntry = String.format("[%s] %s: %s\n", timestamp, role, message);
            
            try (FSDataOutputStream out = fs.append(logFile)) {
                out.write(logEntry.getBytes(StandardCharsets.UTF_8));
            }
            
            logger.debug("Logged message for session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to log message: {}", e.getMessage());
        }
    }
    
    public String storeFile(String sessionId, String fileName, byte[] fileContent) {
        return storeFile(sessionId, fileName, fileContent, "Documents");
    }
    
    public String storeFile(String sessionId, String fileName, byte[] fileContent, String folder) {
        try {
            String fileExtension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
            String storedFileName = sessionId + "_" + System.currentTimeMillis() + fileExtension;
            String filePath = getFilePath(storedFileName, folder);
            Path hdfsPath = new Path(filePath);
            
            try (FSDataOutputStream out = fs.create(hdfsPath, true);
                 ByteArrayInputStream in = new ByteArrayInputStream(fileContent)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("Stored file: {} -> {} in folder {}", fileName, storedFileName, folder);
            return filePath;
            
        } catch (Exception e) {
            logger.error("Failed to store file: {}", e.getMessage());
            return null;
        }
    }
    
    public byte[] getFile(String fileName) {
        try {
            String filePath = getFilePath(fileName);
            Path hdfsPath = new Path(filePath);
            
            if (!fs.exists(hdfsPath)) {
                return null;
            }
            
            try (FSDataInputStream in = fs.open(hdfsPath);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                
                return out.toByteArray();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get file: {}", e.getMessage());
            return null;
        }
    }
    
    public boolean saveChatHistory(String chatId, String title, String messages, String timestamp, int messageCount) {
        try {
            String chatHistoryPath = CHATBOT_BASE_PATH + "/ChatHistory/" + chatId + ".json";
            String jsonContent = String.format(
                "{\"chatId\":\"%s\",\"title\":\"%s\",\"messages\":%s,\"timestamp\":\"%s\",\"messageCount\":%d}",
                chatId, title, messages, timestamp, messageCount
            );
            
            Path chatFile = new Path(chatHistoryPath);
            try (FSDataOutputStream out = fs.create(chatFile, true)) {
                out.write(jsonContent.getBytes(StandardCharsets.UTF_8));
            }
            
            logger.info("Saved chat history to HDFS: {}", chatHistoryPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to save chat history to HDFS: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean deleteChatHistory(String chatId) {
        try {
            String chatHistoryPath = CHATBOT_BASE_PATH + "/ChatHistory/" + chatId + ".json";
            Path chatFile = new Path(chatHistoryPath);
            
            if (fs.exists(chatFile)) {
                boolean deleted = fs.delete(chatFile, false);
                if (deleted) {
                    logger.info("Deleted chat history from HDFS: {}", chatHistoryPath);
                    return true;
                } else {
                    logger.warn("Failed to delete chat history from HDFS: {}", chatHistoryPath);
                    return false;
                }
            } else {
                logger.info("Chat history file not found in HDFS: {}", chatHistoryPath);
                return true; // Not found is considered success
            }
            
        } catch (Exception e) {
            logger.error("Failed to delete chat history from HDFS: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean clearAllChatHistory() {
        try {
            String chatHistoryPath = CHATBOT_BASE_PATH + "/ChatHistory";
            Path chatDir = new Path(chatHistoryPath);
            
            if (fs.exists(chatDir)) {
                boolean deleted = fs.delete(chatDir, true); // true for recursive delete
                if (deleted) {
                    logger.info("Cleared all chat history from HDFS: {}", chatHistoryPath);
                    // Recreate the directory
                    fs.mkdirs(chatDir);
                    return true;
                } else {
                    logger.warn("Failed to clear all chat history from HDFS: {}", chatHistoryPath);
                    return false;
                }
            } else {
                logger.info("Chat history directory not found in HDFS: {}", chatHistoryPath);
                return true; // Not found is considered success
            }
            
        } catch (Exception e) {
            logger.error("Failed to clear all chat history from HDFS: {}", e.getMessage());
            return false;
        }
    }
    
    public void clearSession(String sessionId) {
        try {
            // Remove session file
            String sessionPath = getSessionPath(sessionId);
            Path sessionFile = new Path(sessionPath);
            if (fs.exists(sessionFile)) {
                fs.delete(sessionFile, false);
            }
            
            // Remove log file
            String logPath = getLogPath(sessionId);
            Path logFile = new Path(logPath);
            if (fs.exists(logFile)) {
                fs.delete(logFile, false);
            }
            
            logger.info("Cleared session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to clear session: {}", e.getMessage());
        }
    }
    
    public String getChatHistory(String sessionId) {
        try {
            String logPath = getLogPath(sessionId);
            Path logFile = new Path(logPath);
            
            if (!fs.exists(logFile)) {
                return "";
            }
            
            try (FSDataInputStream in = fs.open(logFile);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                
                return out.toString(StandardCharsets.UTF_8.name());
            }
            
        } catch (Exception e) {
            logger.error("Failed to get chat history: {}", e.getMessage());
            return "";
        }
    }
    
    private String getSessionPath(String sessionId) {
        return CHATBOT_BASE_PATH + "/sessions/" + sessionId + ".json";
    }
    
    private String getLogPath(String sessionId) {
        return CHATBOT_BASE_PATH + "/logs/" + sessionId + ".log";
    }
    
    private String getFilePath(String fileName) {
        return getFilePath(fileName, "Documents");
    }
    
    private String getFilePath(String fileName, String folder) {
        return CHATBOT_BASE_PATH + "/" + folder + "/" + fileName;
    }
    
    public void shutdown() {
        try {
            if (fs != null) {
                fs.close();
            }
            logger.info("HadoopManager shutdown complete");
        } catch (Exception e) {
            logger.error("Error during HadoopManager shutdown: {}", e.getMessage());
        }
    }
}
