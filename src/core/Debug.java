package core;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Debug {
    private static boolean enabled = GameConfig.Debug.ENABLED;
    private static boolean fileLoggingEnabled = GameConfig.Debug.FILE_LOGGING_ENABLED;
    private static String logDirectory = GameConfig.Debug.LOG_DIRECTORY;
    private static Map<String, PrintWriter> playerLogWriters = new ConcurrentHashMap<>();
    private static PrintWriter generalLogWriter;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static ExecutorService loggingExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Debug-Logger-Thread");
        thread.setDaemon(true);
        thread.setPriority(GameConfig.Debug.LOGGING_THREAD_PRIORITY);
        return thread;
    });

    private static BlockingQueue<LogTask> logQueue = new LinkedBlockingQueue<>();

    static {
        initializeLogging();
        startLoggingThread();
    }

    private static void startLoggingThread() {
        loggingExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    LogTask task = logQueue.take();
                    task.execute();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error in logging thread: " + e.getMessage());
                }
            }
        });
    }

    private static class LogTask {
        private final String category;
        private final String message;
        private final boolean isConsole;
        private final boolean isError;
        private final Throwable throwable;

        LogTask(String category, String message, boolean isConsole, boolean isError, Throwable throwable) {
            this.category = category;
            this.message = message;
            this.isConsole = isConsole;
            this.isError = isError;
            this.throwable = throwable;
        }

        void execute() {
            if (isConsole) {
                if (isError) {
                    System.err.println(message);
                } else {
                    System.out.println(message);
                }
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
            writeToFileSync(category, message, throwable);
        }
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
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        closeAllWriters();
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void log(String message) {
        if (enabled) {
            String formattedMessage = "[DEBUG] " + message;
            enqueueLog("GENERAL", formattedMessage, true, false, null);
        }
    }

    public static void log(String message, Throwable throwable) {
        if (enabled) {
            String formattedMessage = "[DEBUG] " + message;
            enqueueLog("GENERAL", formattedMessage, true, false, throwable);
        }
    }

    public static void logPlayer(String playerId, String message) {
        if (enabled) {
            String formattedMessage = "[PLAYER-" + playerId + "] " + message;
            enqueueLog(playerId, formattedMessage, true, false, null);
        }
    }

    public static void logServer(String message) {
        if (enabled) {
            String formattedMessage = "[SERVER] " + message;
            enqueueLog("SERVER", formattedMessage, true, false, null);
        }
    }

    public static void logClient(String message) {
        if (enabled) {
            String formattedMessage = "[CLIENT] " + message;
            enqueueLog("CLIENT", formattedMessage, true, false, null);
        }
    }

    public static void logGameState(String message) {
        if (enabled) {
            String formattedMessage = "[GAME_STATE] " + message;
            enqueueLog("GAME_STATE", formattedMessage, true, false, null);
        }
    }

    public static void logNetwork(String message) {
        if (enabled) {
            String formattedMessage = "[NETWORK] " + message;
            enqueueLog("NETWORK", formattedMessage, true, false, null);
        }
    }

    public static void logTurn(String message) {
        if (enabled) {
            String formattedMessage = "[TURN] " + message;
            enqueueLog("TURN", formattedMessage, true, false, null);
        }
    }

    public static void error(String message) {
        String formattedMessage = "[ERROR] " + message;
        enqueueLog("ERROR", formattedMessage, true, true, null);
    }

    public static void error(String message, Throwable throwable) {
        String formattedMessage = "[ERROR] " + message;
        enqueueLog("ERROR", formattedMessage, true, true, throwable);
    }

    public static void errorPlayer(String playerId, String message) {
        String formattedMessage = "[ERROR-PLAYER-" + playerId + "] " + message;
        enqueueLog(playerId, formattedMessage, true, true, null);
    }

    public static void info(String message) {
        String formattedMessage = "[INFO] " + message;
        enqueueLog("GENERAL", formattedMessage, true, false, null);
    }

    public static void warning(String message) {
        String formattedMessage = "[WARNING] " + message;
        enqueueLog("GENERAL", formattedMessage, true, false, null);
    }

    private static void enqueueLog(String category, String message, boolean isConsole, boolean isError, Throwable throwable) {
        try {
            logQueue.offer(new LogTask(category, message, isConsole, isError, throwable));
        } catch (Exception e) {
            System.err.println("Failed to enqueue log: " + e.getMessage());
        }
    }
    
    private static void writeToFileSync(String category, String message, Throwable throwable) {
        if (!fileLoggingEnabled) return;

        try {
            String timestamp = dateFormat.format(new Date());
            String logEntry = "[" + timestamp + "] " + message;

            if (throwable != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                logEntry += "\n" + sw.toString();
            }

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
        try {
            loggingExecutor.shutdown();
            if (!loggingExecutor.awaitTermination(GameConfig.Debug.SHUTDOWN_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                loggingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            loggingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
