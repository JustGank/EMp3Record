package com.xjl.emp3.demo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.xjl.emp3record_demo.R
import com.xjl.emp3recorder.logger.Logger
import com.xjl.emp3recorder.record_wave.RecordWaveView
import com.xjl.emp3recorder.utils.MediaPlayerHepler
import com.xjl.emp3recorder.utils.WaveRecordBinder
import java.io.File

/**
 * Created by x33664 on 2019/2/21.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName


    private lateinit var mp3CacheDirPath: String

    var waveview: RecordWaveView? = null
    var waveRecordBinder: WaveRecordBinder? = null
    var mediaPlayerHepler: MediaPlayerHepler? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_main)
        val permissions = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 1)
        }

        mp3CacheDirPath = externalCacheDir?.absolutePath + File.separator + "EMp3Recorder"
        Logger.i("$TAG onCreate mp3CacheDirPath : $mp3CacheDirPath")
        val audioDir = File(mp3CacheDirPath)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        waveview = findViewById<View>(R.id.waveview) as RecordWaveView
        waveRecordBinder = WaveRecordBinder(waveview!!, mp3CacheDirPath)
        mediaPlayerHepler = MediaPlayerHepler().apply {
            completeListener=object : MediaPlayer.OnCompletionListener{
                override fun onCompletion(p0: MediaPlayer?) {
                  findViewById<TextView>(R.id.player).text = "开始播放"
                }

            }
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        waveRecordBinder?.onWindowFocusChanged(hasFocus)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayerHepler?.pause()
    }

    var recordaFile: File? = null
    fun recordButton(view: View) {
        waveRecordBinder?.apply {
            if (isRecording) {
                (view as Button).text = "开始录音"
                stop()
            } else {
                (view as Button).text = "停止录音"
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this@MainActivity, "没有录音权限", Toast.LENGTH_SHORT).show()
                    return
                }
                recordaFile = start()
            }
        }
    }

    fun player(view: View) {
        mediaPlayerHepler?.let {
            if (it.isPlaying) {
                it.pause()
                (view as Button).text = "开始播放"
            } else {
                if (it.prepared) {
                    it.start()
                } else {
                    it.start(recordaFile?.absolutePath)
                }
                (view as Button).text = "停止播放"
            }
        }
    }
}
