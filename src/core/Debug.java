package core;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Debug {
    private static boolean enabled = true;
    private static boolean fileLoggingEnabled = true;
    private static String logDirectory = "logs";
    private static Map<String, PrintWriter> playerLogWriters = new ConcurrentHashMap<>();
    private static PrintWriter generalLogWriter;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    static {
        initializeLogging();
    }
    
    private static void initializeLogging() {
        if (!fileLoggingEnabled) return;
        
        try {
            File logDir = new File(logDirectory);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String generalLogFile = logDirectory + File.separator + "general_" + timestamp + ".log";
            generalLogWriter = new PrintWriter(new FileWriter(generalLogFile, true));
            
            log("Debug logging initialized. General log: " + generalLogFile);
        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
            fileLoggingEnabled = false;
        }
    }
    
    public static void enable() {
        enabled = true;
    }
    
    public static void disable() {
        enabled = false;
    }
    
    public static void enableFileLogging() {
        fileLoggingEnabled = true;
        initializeLogging();
    }
    
    public static void disableFileLogging() {
        fileLoggingEnabled = false;
        closeAllWriters();
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void log(String message) {
        if (enabled) {
            String formattedMessage = "[DEBUG] " + message;
            System.out.println(formattedMessage);
            writeToFile("GENERAL", formattedMessage);
        }
    }
    
    public static void log(String message, Throwable throwable) {
        if (enabled) {
            String formattedMessage = "[DEBUG] " + message;
            System.out.println(formattedMessage);
            throwable.printStackTrace();
            writeToFile("GENERAL", formattedMessage + " - Exception: " + throwable.getMessage());
        }
    }
    
    public static void logPlayer(String playerId, String message) {
        if (enabled) {
            String formattedMessage = "[PLAYER-" + playerId + "] " + message;
            System.out.println(formattedMessage);
            writeToFile(playerId, formattedMessage);
        }
    }
    
    public static void logServer(String message) {
        if (enabled) {
            String formattedMessage = "[SERVER] " + message;
            System.out.println(formattedMessage);
            writeToFile("SERVER", formattedMessage);
        }
    }
    
    public static void logClient(String message) {
        if (enabled) {
            String formattedMessage = "[CLIENT] " + message;
            System.out.println(formattedMessage);
            writeToFile("CLIENT", formattedMessage);
        }
    }
    
    public static void logGameState(String message) {
        if (enabled) {
            String formattedMessage = "[GAME_STATE] " + message;
            System.out.println(formattedMessage);
            writeToFile("GAME_STATE", formattedMessage);
        }
    }
    
    public static void logNetwork(String message) {
        if (enabled) {
            String formattedMessage = "[NETWORK] " + message;
            System.out.println(formattedMessage);
            writeToFile("NETWORK", formattedMessage);
        }
    }
    
    public static void logTurn(String message) {
        if (enabled) {
            String formattedMessage = "[TURN] " + message;
            System.out.println(formattedMessage);
            writeToFile("TURN", formattedMessage);
        }
    }
    
    public static void error(String message) {
        String formattedMessage = "[ERROR] " + message;
        System.err.println(formattedMessage);
        writeToFile("ERROR", formattedMessage);
    }
    
    public static void error(String message, Throwable throwable) {
        String formattedMessage = "[ERROR] " + message;
        System.err.println(formattedMessage);
        throwable.printStackTrace();
        writeToFile("ERROR", formattedMessage + " - Exception: " + throwable.getMessage());
    }
    
    public static void errorPlayer(String playerId, String message) {
        String formattedMessage = "[ERROR-PLAYER-" + playerId + "] " + message;
        System.err.println(formattedMessage);
        writeToFile(playerId, formattedMessage);
    }
    
    public static void info(String message) {
        String formattedMessage = "[INFO] " + message;
        System.out.println(formattedMessage);
        writeToFile("GENERAL", formattedMessage);
    }
    
    public static void warning(String message) {
        String formattedMessage = "[WARNING] " + message;
        System.out.println(formattedMessage);
        writeToFile("GENERAL", formattedMessage);
    }
    
    private static void writeToFile(String category, String message) {
        if (!fileLoggingEnabled) return;
        
        try {
            String timestamp = dateFormat.format(new Date());
            String logEntry = "[" + timestamp + "] " + message;
            
            if ("GENERAL".equals(category)) {
                if (generalLogWriter != null) {
                    generalLogWriter.println(logEntry);
                    generalLogWriter.flush();
                }
            } else {
                PrintWriter writer = getPlayerLogWriter(category);
                if (writer != null) {
                    writer.println(logEntry);
                    writer.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
    
    private static PrintWriter getPlayerLogWriter(String playerId) {
        if (!fileLoggingEnabled) return null;
        
        return playerLogWriters.computeIfAbsent(playerId, id -> {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String logFile = logDirectory + File.separator + "player_" + id + "_" + timestamp + ".log";
                return new PrintWriter(new FileWriter(logFile, true));
            } catch (IOException e) {
                System.err.println("Failed to create player log file for " + id + ": " + e.getMessage());
                return null;
            }
        });
    }
    
    private static void closeAllWriters() {
        if (generalLogWriter != null) {
            generalLogWriter.close();
            generalLogWriter = null;
        }
        
        for (PrintWriter writer : playerLogWriters.values()) {
            if (writer != null) {
                writer.close();
            }
        }
        playerLogWriters.clear();
    }
    
    public static void cleanup() {
        closeAllWriters();
    }
    
    public static void setLogDirectory(String directory) {
        logDirectory = directory;
        if (fileLoggingEnabled) {
            initializeLogging();
        }
    }
    
    public static String getLogDirectory() {
        return logDirectory;
    }
}
