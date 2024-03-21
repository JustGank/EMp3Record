package com.xjl.emp3record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xjl.emp3recorder.record_wave.RecordWaveView;
import com.xjl.emp3recorder.utils.MediaPlayerHepler;
import com.xjl.emp3recorder.utils.WaveRecordBinder;
import com.xjl.emp3record_demo.R;
import java.io.File;

/**
 * Created by x33664 on 2019/2/21.
 */

public class WaveAudioRecordActivity extends AppCompatActivity {

    private static final String TAG = WaveAudioRecordActivity.class.getSimpleName();

    protected RecordWaveView waveview;

    protected WaveRecordBinder waveRecordBinder;

    protected MediaPlayerHepler mediaPlayerHepler;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_wave_audio_record);
        waveview = (RecordWaveView) findViewById(R.id.waveview);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                ) {
            Toast.makeText(this, "请给与权限后重试", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        waveRecordBinder = new WaveRecordBinder(waveview, MainActivity.mp3CachePath);

        mediaPlayerHepler = new MediaPlayerHepler(this);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (null != waveRecordBinder)
            waveRecordBinder.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != waveRecordBinder)
            waveRecordBinder.onPause(false, false);

        if (null != mediaPlayerHepler)
            mediaPlayerHepler.stop();

    }

    File recordaFile;

    public void reordButton(View view) {
        if (waveRecordBinder != null) {
            if (waveRecordBinder.isRecording()) {
                ((Button) view).setText("开始录音");
                waveRecordBinder.stop();


            } else {
                ((Button) view).setText("停止录音");
                recordaFile = waveRecordBinder.start();


            }
        }
    }


    public void player(View view) {
        if (mediaPlayerHepler.isPlaying()) {
            mediaPlayerHepler.pause();
            ((Button) view).setText("开始播放");

        } else {
            if (mediaPlayerHepler.getPrepared()) {
                mediaPlayerHepler.start();
            } else {
                mediaPlayerHepler.start(recordaFile.getAbsolutePath());
            }
            ((Button) view).setText("停止播放");
        }
    }


}
