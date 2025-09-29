package org.oosd.ui;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/** Simple one-stop helper for BGM + SFX. */
public final class Sound {

    private Sound() {}

    /* ---------- SFX (short sounds) ---------- */
    private static final Map<String, AudioClip> SFX = new HashMap<>();

    private static AudioClip sfx(String path) {
        return SFX.computeIfAbsent(path, p -> {
            URL url = Sound.class.getResource(p);
            if (url == null) throw new IllegalArgumentException("Missing resource: " + p);
            AudioClip clip = new AudioClip(url.toExternalForm());
            clip.setVolume(1.0); // make sure SFX play loud enough over BGM
            return clip;
        });
    }



    public static void playRotate()   { sfx("/audio/sfx_rotate.mp3").play(); }
    public static void playLine()     { sfx("/audio/sfx_line.mp3").play(); }
    public static void playGameOver() { sfx("/audio/sfx_gameover.wav").play(); }

    /* ---------- BGM (looping music) ---------- */
    private static MediaPlayer bgm;

    public static void startMenuBgm() { startBgm("/audio/bgm_menu.mp3", 0.35); }
    public static void startGameBgm() { startBgm("/audio/bgm_game.mp3", 0.35); }

    public static void stopBgm() {
        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
            bgm = null;
        }
    }

    private static void startBgm(String path, double volume) {
        stopBgm();
        URL url = Sound.class.getResource(path);
        if (url == null) throw new IllegalArgumentException("Missing resource: " + path);
        bgm = new MediaPlayer(new Media(url.toExternalForm()));
        bgm.setCycleCount(MediaPlayer.INDEFINITE);
        bgm.setVolume(volume);
        bgm.play();
    }
}
