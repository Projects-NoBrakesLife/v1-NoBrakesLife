package util;

import core.GameConfig;
import java.awt.*;

public class BackgroundUtil {
    public static Rectangle getBackgroundDest(Image bg) {
        if (GameConfig.Display.BG_STRETCH) {
            return new Rectangle(0, GameConfig.Display.BG_OFFSET_Y,
                    GameConfig.Display.GAME_WIDTH, GameConfig.Display.GAME_HEIGHT - GameConfig.Display.BG_OFFSET_Y);
        } else {
            int imgW = bg.getWidth(null);
            int imgH = bg.getHeight(null);
            double sx = (double) GameConfig.Display.GAME_WIDTH / imgW;
            double sy = (double) GameConfig.Display.GAME_HEIGHT / imgH;
            double scale = Math.min(sx, sy);
            int w = (int) (imgW * scale);
            int h = (int) (imgH * scale);
            int x = (GameConfig.Display.GAME_WIDTH - w) / 2;
            int y = (GameConfig.Display.GAME_HEIGHT - h) / 2 + GameConfig.Display.BG_OFFSET_Y;
            return new Rectangle(x, y, w, h);
        }
    }
}
