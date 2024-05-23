package com.xjl.emp3recorder.mp3record

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Created by x33664 on 2019/2/18.
 * 采样率（sampleRate）：采样率越高声音的还原度越好。
 * 比特率（bitrate）：每秒钟的数据量，越高音质越好。
 * 声道数（channels）：声道的数量，通常只有单声道和双声道，双声道即所谓的立体声。
 * 使用 AudioRecord 而不是VideoRecord 的好处是 AudioRecord的录制过程中是可加工可控制的。
 * 这里使用了LAME进行编码压缩，转换成MP3进行播放。
 * AudioRecord录制完成的是PCM数据是不能直接播放的。
 */
class MP3Recorder(private var mRecordFile: File) {

    //=======================AudioRecord Default Settings=======================
    /**
     * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
     * 22.05KHz、44.1KHz、48KHz三个等级
     * 44100是官方说明所有设备都支持的
     * 模拟器仅支持从麦克风输入8kHz采样率
     * 官方声明只有ENCODING_PCM_16BIT是所有设备都支持的。
     */
    private val DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    private val DEFAULT_SAMPLING_RATE = 44100
    private val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

    /**
     * 下面是对此的封装
     * private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
     */
    private val DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT

    //======================Lame Default Settings=====================
    private val DEFAULT_LAME_MP3_QUALITY = 7

    /**
     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
     */
    private val DEFAULT_LAME_IN_CHANNEL = 1

    /**
     * default Encoded bit rate. MP3 file will be encoded with bit rate 128kbps
     */
    private var DEFAULT_LAME_MP3_BIT_RATE = 128
    private val FRAME_COUNT = 160
    private val MAX_VOLUME = 2000

    private var mOnRecordListener: OnRecordListener? = null
    private var mAudioRecord: AudioRecord? = null
    private var mBufferSize = 0
    private var mPCMBuffer: ShortArray? = null
    private var mEncodeThread: DataEncodeThread? = null
    var isRecording = false
        private set

    //分贝
    var volumeDb = 0
        private set

    //音量
    private var realVolume = 0

    fun setFile(mRecordFile: File) {
        this.mRecordFile = mRecordFile
    }

    private var startTime: Long = 0
    private var duration: Long = 0

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException initAudioRecorder throws
     */
    @OptIn(DelicateCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Throws(IOException::class)
    fun start(maxDuration: Long) {
        if (isRecording) {
            return
        }
        mOnRecordListener?.onStart()
        isRecording = true
        initAudioRecorder()
        mAudioRecord?.apply {
            startRecording()
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    //设置线程权限
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                    startTime = System.currentTimeMillis()
                    while (isRecording) {
                        val readSize = read(mPCMBuffer!!, 0, mBufferSize)
                        if (readSize > 0) {
                            mEncodeThread!!.addTask(mPCMBuffer!!, readSize)
                            calculateRealVolume(mPCMBuffer!!, readSize)
                        }
                        duration = System.currentTimeMillis() - startTime
                        if (maxDuration > 0 && duration >= maxDuration) {
                            isRecording = false
                        }
                    }
                    stop()
                    release()
                    mAudioRecord = null
                    mEncodeThread!!.sendStopMessage()
                    mOnRecordListener?.onStop(mRecordFile, duration)
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    /**
     * 此计算方法来自samsung开发范例
     *
     * @param buffer buffer
     * @param readSize readSize
     */
    private fun calculateRealVolume(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        if (readSize > 0) {
            val amplitude = sum / readSize
            volumeDb = (10 * log10(amplitude)).toInt()
            realVolume = sqrt(amplitude).toInt()
            if (mOnRecordListener != null) {
                mOnRecordListener!!.onRecording(volumeDb, realVolume)
            }
        }
    }


    val volume: Int
        /**
         * 获取相对音量。 超过最大值时取最大值。
         *
         * @return 音量
         */
        get() = if (realVolume >= MAX_VOLUME) {
            MAX_VOLUME
        } else realVolume

    fun stop() {
        isRecording = false
    }

    /**
     * Initialize audio recorder
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Throws(IOException::class)
    private fun initAudioRecorder() {
        mBufferSize = AudioRecord.getMinBufferSize(
            DEFAULT_SAMPLING_RATE,
            DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.audioFormat
        )
        val bytesPerFrame = DEFAULT_AUDIO_FORMAT.bytesPerFrame

        var frameSize = mBufferSize / bytesPerFrame
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += FRAME_COUNT - frameSize % FRAME_COUNT
            mBufferSize = frameSize * bytesPerFrame
        }

        mAudioRecord = AudioRecord(
            DEFAULT_AUDIO_SOURCE,
            DEFAULT_SAMPLING_RATE,
            DEFAULT_CHANNEL_CONFIG,
            DEFAULT_AUDIO_FORMAT.audioFormat,
            mBufferSize
        )
        mPCMBuffer = ShortArray(mBufferSize)
        LameUtil.init(
            DEFAULT_SAMPLING_RATE,
            DEFAULT_LAME_IN_CHANNEL,
            DEFAULT_SAMPLING_RATE,
            DEFAULT_LAME_MP3_BIT_RATE,
            DEFAULT_LAME_MP3_QUALITY
        )

        mEncodeThread = DataEncodeThread(mRecordFile, mBufferSize).apply {
            start()
        }

        mAudioRecord?.let {
            it.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread!!.mHandler)
            it.setPositionNotificationPeriod(FRAME_COUNT)
        }
    }

    fun setDefaultLameMp3BitRate(rate: Int) {
        DEFAULT_LAME_MP3_BIT_RATE = rate
    }

    interface OnRecordListener {
        fun onStart()
        fun onStop(file: File?, duration: Long)
        fun onRecording(mVolumeDb: Int, mVolume: Int)
    }

    fun setOnRecordListener(listener: OnRecordListener?) {
        mOnRecordListener = listener
    }


}
