import java.awt.*;

public class BackgroundUtil {
    public static Rectangle getBackgroundDest(Image bg) {
        if (Config.BG_STRETCH) {
            return new Rectangle(0, Config.BG_OFFSET_Y,
                    Config.GAME_WIDTH, Config.GAME_HEIGHT - Config.BG_OFFSET_Y);
        } else {
            int imgW = bg.getWidth(null);
            int imgH = bg.getHeight(null);
            double sx = (double) Config.GAME_WIDTH / imgW;
            double sy = (double) Config.GAME_HEIGHT / imgH;
            double scale = Math.min(sx, sy);
            int w = (int) (imgW * scale);
            int h = (int) (imgH * scale);
            int x = (Config.GAME_WIDTH - w) / 2;
            int y = (Config.GAME_HEIGHT - h) / 2 + Config.BG_OFFSET_Y;
            return new Rectangle(x, y, w, h);
        }
    }
}
