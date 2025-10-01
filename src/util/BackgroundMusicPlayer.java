package util;

import core.Debug;
import core.Lang;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class BackgroundMusicPlayer {
    private Clip backgroundClip;
    private boolean isPlaying = false;
    private float volume = 0.70f; 

    public BackgroundMusicPlayer() {
        initializeBackgroundMusic();
    }

    private void initializeBackgroundMusic() {
        try {
            File musicFile = new File(Lang.BACKGROUND_MUSIC);
            System.out.println("Looking for background music at: " + musicFile.getAbsolutePath());
            
            if (!musicFile.exists()) {
                Debug.error("Background music file not found: " + Lang.BACKGROUND_MUSIC, null);
                System.out.println("File does not exist!");
                return;
            }
            
            System.out.println("Background music file found, size: " + musicFile.length() + " bytes");

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioStream);
            
            System.out.println("Background music clip opened successfully");
         
            if (backgroundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gainControl.getMaximum() - gainControl.getMinimum();
                float gain = (range * volume) + gainControl.getMinimum();
                gainControl.setValue(gain);
                System.out.println("Volume set to: " + volume + " (gain: " + gain + ")");
            } else {
                System.out.println("MASTER_GAIN control not supported");
            }
          
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            System.out.println("Background music initialized and ready to play");
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            Debug.error("Failed to initialize background music: " + Lang.BACKGROUND_MUSIC, e);
            System.out.println("Error initializing background music: " + e.getMessage());
        }
    }

    public void play() {
        System.out.println("Attempting to play background music...");
        if (backgroundClip != null && !isPlaying) {
            System.out.println("Starting background music playback");
            backgroundClip.setFramePosition(0);
            backgroundClip.start();
            isPlaying = true;
            System.out.println("Background music is now playing");
        } else if (backgroundClip == null) {
            System.out.println("Background clip is null - cannot play");
        } else if (isPlaying) {
            System.out.println("Background music is already playing");
        }
    }

    public void stop() {
        if (backgroundClip != null && isPlaying) {
            backgroundClip.stop();
            isPlaying = false;
        }
    }

    public void pause() {
        if (backgroundClip != null && isPlaying) {
            backgroundClip.stop();
            isPlaying = false;
        }
    }

    public void resume() {
        if (backgroundClip != null && !isPlaying) {
            backgroundClip.start();
            isPlaying = true;
        }
    }

    public void setVolume(float newVolume) {
        if (newVolume >= 0.0f && newVolume <= 1.0f) {
            this.volume = newVolume;
            if (backgroundClip != null && backgroundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gainControl.getMaximum() - gainControl.getMinimum();
                float gain = (range * volume) + gainControl.getMinimum();
                gainControl.setValue(gain);
            }
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void dispose() {
        if (backgroundClip != null) {
            backgroundClip.close();
            backgroundClip = null;
        }
    }
}
