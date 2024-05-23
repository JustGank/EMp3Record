package com.xjl.emp3recorder.utils

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.xjl.emp3recorder.logger.Logger
import com.xjl.emp3recorder.mp3record.MP3Recorder
import com.xjl.emp3recorder.mp3record.MP3Recorder.OnRecordListener
import com.xjl.emp3recorder.record_wave.RecordWaveView
import com.xjl.emp3recorder.record_wave.RecordWaveView.RefreshAmplitude
import java.io.File
import java.io.IOException

class WaveRecordBinder(
    private val waveview: RecordWaveView,
    /**
     * 文件存储的地址
     */
    private val filePath: String
) {
    private val TAG = "WaveRecordBinder"

    var mp3Recorder: MP3Recorder?
        private set

    var currentRecordFile: File? = null
        private set

    private val mp3Endcase = ".mp3"

    var recordListener: OnRecordListener = object : OnRecordListener {
        override fun onStart() {
            Logger.i("$TAG start recording!")
            waveview.startAnim()
        }

        override fun onStop(file: File?, duration: Long) {
            Logger.i("$TAG stop recording file is exist=" + (file != null) + " duration=" + duration)
            waveview.stopAnim()
        }

        override fun onRecording(mVolumeDb: Int, mVolume: Int) {}
    }
    private var refreshAmplitude: RefreshAmplitude = object : RefreshAmplitude {
        override fun refresh() {
            val db: Int = volumeDb
            Logger.i("$TAG RefreshAmplitude refresh volumeDb = $db")
            waveview.setVolume(db)
        }
    }

    /**
     * 自动生成地址
     */
    init {
        val file = File(filePath)
        if (!file.exists()) {
            file.mkdirs()
        }
        mp3Recorder = MP3Recorder(file).apply {
            setOnRecordListener(recordListener)
        }
        waveview.setRefreshAmplitude(refreshAmplitude)
    }

    val volumeDb: Int
        get() = mp3Recorder?.volumeDb ?: 0

    val volume: Int
        get() = mp3Recorder?.volume ?: 0

    val isRecording: Boolean
        get() = mp3Recorder?.isRecording == true

    /**
     * 绑定相关的生命周期
     */
    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && isRecording) {
            waveview.startAnim()
        }
    }

    @JvmOverloads
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(name: String = System.currentTimeMillis().toString() + mp3Endcase): File? {
        if (isRecording) {
            return null
        }
        val tempFilePath = filePath + name + if (name.endsWith(mp3Endcase)) "" else mp3Endcase
        currentRecordFile = File(tempFilePath)
        if (null == mp3Recorder) {
            mp3Recorder = MP3Recorder(currentRecordFile!!)
        } else {
            mp3Recorder!!.setFile(currentRecordFile!!)
        }
        try {
            mp3Recorder!!.start(-1)
            waveview.startAnim()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return currentRecordFile
    }



    fun stop() {
        waveview.stopAnim()
        if (mp3Recorder != null && mp3Recorder!!.isRecording) {
            mp3Recorder!!.stop()
            if (mp3Recorder != null) {
                mp3Recorder!!.stop()
            }
        }
    }


}
