package util;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class SoundPlayer {
    private long lastPlayTime = 0;
    private static final long MIN_PLAY_INTERVAL = 100; 

    public SoundPlayer() {
    }

    public void play(String filePath) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayTime < MIN_PLAY_INTERVAL) {
            return; 
        }
        
        try {
            File soundFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.setFramePosition(0);
            clip.start();
            lastPlayTime = currentTime;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
        }
    }
}
