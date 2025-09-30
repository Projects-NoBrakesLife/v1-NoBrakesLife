package core;

public class Debug {
    private static boolean enabled = true;
    
    public static void enable() {
        enabled = true;
    }
    
    public static void disable() {
        enabled = false;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void log(String message) {
        if (enabled) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    public static void log(String message, Throwable throwable) {
        if (enabled) {
            System.out.println("[DEBUG] " + message);
            throwable.printStackTrace();
        }
    }
    
    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
    
    public static void error(String message, Throwable throwable) {
        System.err.println("[ERROR] " + message);
        throwable.printStackTrace();
    }
    
    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }
    
    public static void warning(String message) {
        System.out.println("[WARNING] " + message);
    }
}
