package com.xjl.emp3recorder.utils

import android.app.Activity
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import java.io.IOException

/**
 * Created by x33664 on 2019/3/7.
 */
class MediaPlayerHepler() {
    private var currentPath: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            isLooping = false
            onErrorListener?.let { setOnErrorListener(it) }
            completeListener?.let { setOnCompletionListener(it) }
        }
    }

    var prepared = false
        private set

    fun start(filePath: String?): Int {
        if (mediaPlayer == null) {
            initMediaPlayer()
            currentPath = filePath
            prepared = try {
                mediaPlayer!!.setDataSource(filePath)
                mediaPlayer!!.prepare()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                return -1
            }
        }
        mediaPlayer!!.start()
        return mediaPlayer!!.duration
    }

    fun start() {
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        prepared = false
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    val currentPosition: Long
        get() = mediaPlayer?.currentPosition?.toLong() ?: -1

    var completeListener: OnCompletionListener? = null

    var onErrorListener: MediaPlayer.OnErrorListener? = null

}
