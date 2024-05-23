package com.xjl.emp3recorder.utils;

import android.app.Activity;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by x33664 on 2019/3/7.
 */

public class MediaPlayerHepler {

    private Activity activity;

    private String currentPath;

    private MediaPlayer mediaPlayer;

    public MediaPlayerHepler(Activity activity) {
        this.activity = activity;
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(false);
        if (this.onErrorListener != null)
            mediaPlayer.setOnErrorListener(this.onErrorListener);
        if (this.completeListener != null)
            mediaPlayer.setOnCompletionListener(this.completeListener);
    }

    private boolean prepared = false;

    public int start(String filePath) {
        if (mediaPlayer == null) {
            initMediaPlayer();
            this.currentPath = filePath;
            try {
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepare();
                prepared = true;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }
        mediaPlayer.start();
        return mediaPlayer.getDuration();
    }

    public boolean getPrepared() {
        return prepared;
    }

    public void start() {
        if (null != mediaPlayer) {
            mediaPlayer.start();
        }
    }

    public void pause() {
        if (null != mediaPlayer) {
            mediaPlayer.pause();
        }
    }

    public void stop() {
        if (null != mediaPlayer) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            prepared = false;
        }
    }


    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }


    public long getCurrentPosition() {
        if (null != mediaPlayer) {
            return mediaPlayer.getCurrentPosition();
        }
        return -1;
    }

    private MediaPlayer.OnCompletionListener completeListener;

    public void setOnCompleteListener(MediaPlayer.OnCompletionListener completeListener) {
        this.completeListener = completeListener;
    }

    private MediaPlayer.OnErrorListener onErrorListener;

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

}
