package com.xjl.emp3recorder.utils

import android.Manifest
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
    private val cacheFileDir: String
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

        override fun onStop(file: File, duration: Long) {
            Logger.i("$TAG stop recording file is exist= ${file.exists()} duration=$duration"  )
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
        val file = File(cacheFileDir)
        if (!file.exists()) {
            file.mkdirs()
        }
        mp3Recorder = MP3Recorder().apply {
            setOnRecordListener(recordListener)
        }
        waveview.setRefreshAmplitude(refreshAmplitude)
    }

    val volumeDb: Int
        get() = mp3Recorder?.volumeDb ?: 0

    val volume: Int
        get() = mp3Recorder?.volume ?: 0

    var isRecording: Boolean = false
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
    fun start(fileName: String = System.currentTimeMillis().toString() + mp3Endcase): File? {
        if (isRecording) {
            return null
        }
        val tempFilePath =
            cacheFileDir + File.separator + fileName + if (fileName.endsWith(mp3Endcase)) "" else mp3Endcase
        currentRecordFile = File(tempFilePath)
        if (null == mp3Recorder) {
            mp3Recorder = MP3Recorder( )
        }
        try {
            mp3Recorder!!.start(-1, currentRecordFile!!)
            waveview.startAnim()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return currentRecordFile
    }


    fun stop() {
        waveview.stopAnim()
        mp3Recorder?.let { if(isRecording) it.stop() }
        isRecording=false
    }


}
