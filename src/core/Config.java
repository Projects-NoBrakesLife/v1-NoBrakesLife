package core;

import java.awt.Dimension;
import java.awt.Toolkit;

public class Config {
    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private static final int MIN_WIDTH = 1280;
    private static final int MIN_HEIGHT = 720;

    public static final int GAME_WIDTH = Math.max(MIN_WIDTH, screenSize.width - 100);
    public static final int GAME_HEIGHT = Math.max(MIN_HEIGHT, screenSize.height - 100);

    public static final int BG_OFFSET_Y = -150;

    public static final boolean BG_STRETCH = true;

    public static final double UI_SCALE = calculateScale();
    public static final int UI_OFFSET_Y = 40;

    private static double calculateScale() {
        if (GAME_WIDTH <= 1366)
            return 0.4;
        if (GAME_WIDTH <= 1600)
            return 0.35;
        return 0.3;
    }
}
