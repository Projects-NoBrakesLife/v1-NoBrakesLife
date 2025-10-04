package core;

import java.awt.Point;
import java.util.List;


public class Config {
    public static final int GAME_WIDTH = 1600;
    public static final int GAME_HEIGHT = 900;
    
    public static final int BG_OFFSET_Y = -150;
    public static final boolean BG_STRETCH = true;

    public static final double UI_SCALE = 0.3;
    public static final int UI_OFFSET_Y = 50;
    
    public static final int MIN_PLAYERS_TO_START = 3;

    public static final Point APARTMENT_POINT = new Point(779, 250);
    public static final Point BANK_POINT = new Point(1202, 480);
    public static final Point TECH_POINT = new Point(1242, 606);
    public static final Point JOB_OFFICE_POINT = new Point(1018, 650);
    public static final Point CULTURE_POINT = new Point(769, 583);
    public static final Point UNIVERSITY_POINT = new Point(539, 626);

    public static final List<Point> ROAD_POINTS = List.of(
        new Point(781, 253),
        new Point(854, 251),
        new Point(936, 247),
        new Point(1002, 244),
        new Point(1027, 261),
        new Point(1044, 307),
        new Point(1052, 345),
        new Point(1069, 388),
        new Point(1075, 438),
        new Point(1095, 463),
        new Point(1139, 476),
        new Point(1186, 476),
        new Point(1154, 505),
        new Point(1159, 533),
        new Point(1176, 569),
        new Point(1185, 598),
        new Point(1199, 625),
        new Point(1205, 654),
        new Point(1199, 668),
        new Point(1161, 673),
        new Point(1124, 673),
        new Point(1104, 677),
        new Point(1081, 676),
        new Point(1050, 668),
        new Point(1018, 677),
        new Point(1003, 697),
        new Point(973, 687),
        new Point(939, 665),
        new Point(908, 629),
        new Point(883, 602),
        new Point(819, 598),
        new Point(782, 594),
        new Point(727, 596),
        new Point(693, 612),
        new Point(669, 646),
        new Point(639, 671),
        new Point(617, 697),
        new Point(598, 694),
        new Point(575, 675),
        new Point(546, 666),
        new Point(515, 680),
        new Point(488, 697),
        new Point(454, 687),
        new Point(416, 644),
        new Point(401, 595),
        new Point(408, 546),
        new Point(411, 494),
        new Point(416, 461),
        new Point(438, 411),
        new Point(463, 365),
        new Point(487, 316),
        new Point(501, 286),
        new Point(528, 259),
        new Point(568, 257),
        new Point(600, 254),
        new Point(648, 252),
        new Point(688, 251),
        new Point(724, 249),
        new Point(754, 250),
        new Point(772, 249)
    );
    
    public static final String FONT_PATH_THAI = "./assets/font/Arabica.ttf";
    public static final String FONT_PATH_ENGLISH = "./assets/font/GoodDog_New.otf";
    public static final String DEFAULT_FONT_NAME = "SansSerif";
    
    public static final long SOUND_MIN_PLAY_INTERVAL = 100;
    
    public static final int NETWORK_UPDATE_INTERVAL = 200; 
    public static final int NETWORK_POSITION_UPDATE_INTERVAL = 300;
    public static final int NETWORK_STATS_UPDATE_INTERVAL = 800; 
    public static final int NETWORK_TIME_UPDATE_INTERVAL = 1500; 
    
    public static final int MOVEMENT_TIMER_INTERVAL = 8;
    public static final double HOVER_SCALE = 1.05;
    
    public static final int CHARACTER_WIDTH = 64;
    public static final int CHARACTER_HEIGHT = 64;
    
    public static final int STARTING_MONEY = 1000;
    public static final int STARTING_HEALTH = 100;
    public static final int STARTING_ENERGY = 100;
    
    public static final int REST_HEALTH_GAIN = 20;
    public static final int REST_ENERGY_GAIN = 30;
    
    public static final int FONT_SIZE_TITLE = 24;
    public static final int FONT_SIZE_BUTTON = 16;
    public static final int FONT_SIZE_SMALL = 12;
    public static final int FONT_SIZE_MEDIUM = 16;
    public static final int FONT_SIZE_LARGE = 20;
    public static final int FONT_SIZE_EXTRA_LARGE = 36;
    
    public static final int WINDOW_WIDTH_APARTMENT = 1280;
    public static final int WINDOW_HEIGHT_APARTMENT = 720;
    public static final int WINDOW_WIDTH_DEFAULT = 600;
    public static final int WINDOW_HEIGHT_DEFAULT = 400;
    public static final int WINDOW_WIDTH_SHOP = 700;
    public static final int WINDOW_HEIGHT_SHOP = 500;
    public static final int WINDOW_WIDTH_SMALL = 400;
    public static final int WINDOW_HEIGHT_SMALL = 300;
    
    public static final int BUTTON_WIDTH_REST = 200;
    public static final int BUTTON_HEIGHT_REST = 70;
    public static final int BUTTON_MARGIN_REST = 120;
    
    public static final int TURN_TIME_HOURS = 24;
    public static final int TIME_PER_MOVEMENT = 10;
    public static final int TIME_DISPLAY_FONT_SIZE = 20;
}
