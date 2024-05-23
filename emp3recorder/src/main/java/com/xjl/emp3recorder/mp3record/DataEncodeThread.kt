package com.xjl.emp3recorder.mp3record

import android.media.AudioRecord
import android.media.AudioRecord.OnRecordPositionUpdateListener
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

import android.os.Message

import com.xjl.emp3recorder.logger.Logger
import com.xjl.emp3recorder.mp3record.LameUtil.close
import com.xjl.emp3recorder.mp3record.LameUtil.encode
import com.xjl.emp3recorder.mp3record.LameUtil.flush
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections


class DataEncodeThread(file: File?, bufferSize: Int) : HandlerThread("DataEncodeThread"),
    OnRecordPositionUpdateListener {
    private val TAG = "DataEncodeThread"

      var mHandler: StopHandler? = null
    private val mMp3Buffer: ByteArray
    private val mFileOutputStream: FileOutputStream?

    inner class StopHandler(looper: Looper, private val encodeThread: DataEncodeThread) :
        Handler(looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == PROCESS_STOP) {
                //处理缓冲区中的数据
                while (encodeThread.processData() > 0);
                run { removeCallbacksAndMessages(null) }
                // Cancel any event left in the queue
                encodeThread.flushAndRelease()
                looper.quit()
            }
        }
    }

    @Synchronized
    override fun start() {
        super.start()
        mHandler = StopHandler(looper, this)
    }

    fun sendStopMessage() {
        mHandler?.sendEmptyMessage(PROCESS_STOP)
            ?: Logger.w("$TAG sendStopMessage mHandler is null!")
    }



    override fun onMarkerReached(recorder: AudioRecord) {
        // Do nothing
    }

    override fun onPeriodicNotification(recorder: AudioRecord) {
        Logger.d("$TAG onPeriodicNotification")
        processData()
    }

    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     * @return  从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    private fun processData(): Int {
        Logger.d("$TAG processData mTasks.size : $mTasks.size ")
        if (mTasks.size > 0) {
            val task = mTasks.removeAt(0)
            val buffer: ShortArray = task.rawData
            val readSize: Int = task.readSize
            val encodedSize = encode(buffer, buffer, readSize, mMp3Buffer)
            if (encodedSize > 0) {
                try {
                    mFileOutputStream?.write(mMp3Buffer, 0, encodedSize)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return readSize
        }
        return 0
    }

    /**
     * Flush all data left in lame buffer to file
     */
    private fun flushAndRelease() {
        //将MP3结尾信息写入buffer中
        val flushResult = flush(mMp3Buffer)
        if (flushResult > 0) {
            try {
                mFileOutputStream!!.write(mMp3Buffer, 0, flushResult)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                close()
            }
        }
    }

    private val mTasks = Collections.synchronizedList(ArrayList<Task>())

    /**
     * Constructor
     * @param file file
     * @param bufferSize bufferSize
     * @throws FileNotFoundException file not found
     */
    init {
        mFileOutputStream = FileOutputStream(file)
        mMp3Buffer = ByteArray((7200 + bufferSize * 2 * 1.25).toInt())
    }

    fun addTask(rawData: ShortArray, readSize: Int) {
        mTasks.add(Task(rawData, readSize))
    }

    inner class Task(val rawData: ShortArray, val readSize: Int)

    companion object {
        private const val PROCESS_STOP = 1
    }
}
