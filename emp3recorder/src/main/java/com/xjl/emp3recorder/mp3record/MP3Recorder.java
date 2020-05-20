package com.xjl.emp3recorder.mp3record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;

import java.io.File;
import java.io.IOException;

/**
 * Created by x33664 on 2019/2/18.
 * 采样率（sampleRate）：采样率越高声音的还原度越好。
 * 比特率（bitrate）：每秒钟的数据量，越高音质越好。
 * 声道数（channels）：声道的数量，通常只有单声道和双声道，双声道即所谓的立体声。
 * 使用 AudioRecord 而不是VideoRecord 的好处是 AudioRecord的录制过程中是可加工可控制的。
 * 这里使用了LAME进行编码压缩，转换成MP3进行播放。
 * AudioRecord录制完成的是PCM数据是不能直接播放的。
 */

public class MP3Recorder {
    private OnRecordListener mOnRecordListener;
    //=======================AudioRecord Default Settings=======================
    /**
     * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
     * 22.05KHz、44.1KHz、48KHz三个等级
     * 44100是官方说明所有设备都支持的
     * 模拟器仅支持从麦克风输入8kHz采样率
     * 官方声明只有ENCODING_PCM_16BIT是所有设备都支持的。
     */
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int DEFAULT_SAMPLING_RATE = 44100;//
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 下面是对此的封装
     * private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
     */
    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    //======================Lame Default Settings=====================
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
     */
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    /**
     * default Encoded bit rate. MP3 file will be encoded with bit rate 128kbps
     */
    private static int DEFAULT_LAME_MP3_BIT_RATE = 128;

    private static final int FRAME_COUNT = 160;
    private AudioRecord mAudioRecord = null;
    private int mBufferSize;
    private short[] mPCMBuffer;
    private DataEncodeThread mEncodeThread;
    private boolean mIsRecording = false;
    private File mRecordFile;
    //分贝
    private int mVolumeDb;
    //音量
    private int mVolume;

    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     *
     * @param recordFile target file
     */
    public MP3Recorder(File recordFile) {
        mRecordFile = recordFile;
    }

    public void setFile(File mRecordFile) {
        this.mRecordFile = mRecordFile;
    }

    private long startTime = 0, duration = 0;

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException initAudioRecorder throws
     */
    public void start(final long maxDuration) throws IOException {
        if (mIsRecording) {
            return;
        }

        if (mOnRecordListener != null) {
            mOnRecordListener.onStart();
        }

        mIsRecording = true; // 提早，防止init或startRecording被多次调用
        initAudioRecorder();
        mAudioRecord.startRecording();
        new Thread() {
            @Override
            public void run() {
                try {
                    //设置线程权限
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                    startTime = System.currentTimeMillis();
                    while (mIsRecording) {
                        int readSize = mAudioRecord.read(mPCMBuffer, 0, mBufferSize);
                        if (readSize > 0) {
                            mEncodeThread.addTask(mPCMBuffer, readSize);
                            calculateRealVolume(mPCMBuffer, readSize);
                        }
                        duration = System.currentTimeMillis() - startTime;
                        if(maxDuration>0&&duration>=maxDuration){
                            mIsRecording=false;
                        }
                    }

                    // release and finalize audioRecord
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                    // stop the encoding thread and try to wait
                    // until the thread finishes its job
                    mEncodeThread.sendStopMessage();

                    if (mOnRecordListener != null) {
                        mOnRecordListener.onStop(mRecordFile, duration);
                    }

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }

            /**
             * 此计算方法来自samsung开发范例
             *
             * @param buffer buffer
             * @param readSize readSize
             */
            private void calculateRealVolume(short[] buffer, int readSize) {
                double sum = 0;
                for (int i = 0; i < readSize; i++) {
                    // 这里没有做运算的优化，为了更加清晰的展示代码
                    sum += buffer[i] * buffer[i];
                }
                if (readSize > 0) {
                    double amplitude = sum / readSize;
                    mVolumeDb = (int) (10 * Math.log10(amplitude));
                    mVolume = (int) Math.sqrt(amplitude);

                    if(mOnRecordListener != null){
                        mOnRecordListener.onRecording(mVolumeDb,mVolume);
                    }

                }
            }
        }.start();
    }


    /**
     * 获取真实的音量。 [算法来自三星]
     *
     * @return 真实音量
     */
    public int getRealVolume() {
        return mVolume;
    }

    /**
     * 获取当前分贝
     *
     * @return 分贝
     */
    public int getVolumeDb() {
        return mVolumeDb;
    }

    /**
     * 获取相对音量。 超过最大值时取最大值。
     *
     * @return 音量
     */
    public int getVolume() {
        if (mVolume >= MAX_VOLUME) {
            return MAX_VOLUME;
        }
        return mVolume;
    }

    private static final int MAX_VOLUME = 2000;

    /**
     * 根据资料假定的最大值。 实测时有时超过此值。
     *
     * @return 最大音量值。
     */
    public int getMaxVolume() {
        return MAX_VOLUME;
    }

    public void stop() {
        mIsRecording = false;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     * Initialize audio recorder
     */
    private void initAudioRecorder() throws IOException {
        mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
        /* Get number of samples. Calculate the buffer size
         * (round up to the factor of given frame size)
		 * 使能被整除，方便下面的周期性通知
		 * */
        int frameSize = mBufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            mBufferSize = frameSize * bytesPerFrame;
        }

		/* Setup audio recorder */
        mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                mBufferSize);

        mPCMBuffer = new short[mBufferSize];
        /*
         * Initialize lame buffer
		 * mp3 sampling rate is the same as the recorded pcm sampling rate
		 * The bit rate is 32kbps
		 *
		 */
        LameUtil.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
        // Create and run thread used to encode data
        // The thread will
        mEncodeThread = new DataEncodeThread(mRecordFile, mBufferSize);
        mEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    public void setDefaultLameMp3BitRate(int rate) {
        DEFAULT_LAME_MP3_BIT_RATE = rate;
    }

    public interface OnRecordListener {

        void onStart();

        void onStop(File file, long duration);

        public void onRecording(int mVolumeDb,int mVolume);
    }

    public void setOnRecordListener(OnRecordListener listener) {
        mOnRecordListener = listener;
    }
}
