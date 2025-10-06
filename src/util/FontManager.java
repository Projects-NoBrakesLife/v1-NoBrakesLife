package util;

import core.GameConfig;
import core.Debug;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;

public class FontManager {
    private static Font thaiFont;
    private static Font englishFont;
    private static boolean thaiFontLoaded = false;
    private static boolean englishFontLoaded = false;
    
    public static Font loadThaiFont() {
        if (thaiFontLoaded && thaiFont != null) {
            return thaiFont;
        }
        
        try {
            File fontFile = new File(GameConfig.Font.PATH_THAI);
            if (fontFile.exists()) {
                thaiFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                thaiFontLoaded = true;
                Debug.log("Thai font loaded successfully: " + GameConfig.Font.PATH_THAI);
                return thaiFont;
            } else {
                Debug.warning("Thai font file not found: " + GameConfig.Font.PATH_THAI);
                return getDefaultFont();
            }
        } catch (FontFormatException | IOException e) {
            Debug.error("Failed to load Thai font: " + GameConfig.Font.PATH_THAI, e);
            try {
                thaiFont = new Font("Tahoma", Font.PLAIN, 12);
                thaiFontLoaded = true;
                Debug.log("Using fallback Thai font: Tahoma");
                return thaiFont;
            } catch (Exception ex) {
                Debug.error("Failed to load fallback Thai font", ex);
                return getDefaultFont();
            }
        }
    }
    
    public static Font loadEnglishFont() {
        if (englishFontLoaded && englishFont != null) {
            return englishFont;
        }
        
        try {
            File fontFile = new File(GameConfig.Font.PATH_ENGLISH);
            if (fontFile.exists()) {
                try {
                    englishFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                } catch (FontFormatException ex) {
                    englishFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                }
                englishFontLoaded = true;
                Debug.log("English font loaded successfully: " + GameConfig.Font.PATH_ENGLISH);
                return englishFont;
            } else {
                Debug.warning("English font file not found: " + GameConfig.Font.PATH_ENGLISH);
                return getDefaultFont();
            }
        } catch (FontFormatException | IOException e) {
            Debug.error("Failed to load English font: " + GameConfig.Font.PATH_ENGLISH, e);
            try {
                englishFont = new Font("Arial", Font.PLAIN, 12);
                englishFontLoaded = true;
                Debug.log("Using fallback English font: Arial");
                return englishFont;
            } catch (Exception ex) {
                Debug.error("Failed to load fallback English font", ex);
                return getDefaultFont();
            }
        }
    }
    
    public static Font getDefaultFont() {
        Font arabicaFont = loadThaiFont();
        if (arabicaFont != null) {
            return arabicaFont.deriveFont((float) GameConfig.Font.SIZE_SMALL);
        }
        try {
            return new Font("Tahoma", Font.PLAIN, GameConfig.Font.SIZE_SMALL);
        } catch (Exception e) {
            return new Font(GameConfig.Font.DEFAULT_NAME, Font.PLAIN, GameConfig.Font.SIZE_SMALL);
        }
    }
    
    public static Font getThaiFont(int size) {
        Font baseFont = loadThaiFont();
        if (baseFont != null) {
            return baseFont.deriveFont((float) size);
        }
        return new Font(GameConfig.Font.DEFAULT_NAME, Font.PLAIN, size);
    }

    public static Font getThaiFont(int size, int style) {
        Font baseFont = loadThaiFont();
        if (baseFont != null) {
            return baseFont.deriveFont(style, (float) size);
        }
        return new Font(GameConfig.Font.DEFAULT_NAME, style, size);
    }

    public static Font getEnglishFont(int size) {
        Font baseFont = loadEnglishFont();
        if (baseFont != null) {
            return baseFont.deriveFont((float) size);
        }
        return new Font(GameConfig.Font.DEFAULT_NAME, Font.PLAIN, size);
    }

    public static Font getEnglishFont(int size, int style) {
        Font baseFont = loadEnglishFont();
        if (baseFont != null) {
            return baseFont.deriveFont(style, (float) size);
        }
        return new Font(GameConfig.Font.DEFAULT_NAME, style, size);
    }
    
    public static boolean isThaiText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (c >= 0x0E00 && c <= 0x0E7F) {
                return true;
            }
        }
        return false;
    }
    
    public static Font getFontForText(String text, int size) {
        if (isThaiText(text)) {
            return getThaiFont(size);
        } else {
            return getEnglishFont(size);
        }
    }
    
    public static Font getFontForText(String text, int size, int style) {
        if (isThaiText(text)) {
            return getThaiFont(size, style);
        } else {
            return getEnglishFont(size, style);
        }
    }
    
    public static Font getGameFont(int size) {
        return getThaiFont(size);
    }
    
    public static Font getGameFont(int size, int style) {
        return getThaiFont(size, style);
    }
    
    public static boolean isThaiFontLoaded() {
        return thaiFontLoaded && thaiFont != null;
    }
    
    public static boolean isEnglishFontLoaded() {
        return englishFontLoaded && englishFont != null;
    }
    
    public static void reset() {
        thaiFontLoaded = false;
        englishFontLoaded = false;
        thaiFont = null;
        englishFont = null;
    }
    
    public static Font getSmartThaiFont(int size) {
        Font arabicaFont = loadThaiFont();
        if (arabicaFont != null) {
            return arabicaFont.deriveFont((float) size);
        }
        return new Font("Tahoma", Font.PLAIN, size);
    }
    
    public static Font getSmartThaiFont(int size, int style) {
        Font arabicaFont = loadThaiFont();
        if (arabicaFont != null) {
            return arabicaFont.deriveFont(style, (float) size);
        }
        return new Font("Tahoma", style, size);
    }
}
