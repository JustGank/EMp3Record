package com.xjl.emp3record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xjl.emp3recorder.record_wave.RecordWaveView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    protected RecordWaveView waveview;

    private static final String cacheDirPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "EMedia";

    public static String mp3CachePath = cacheDirPath + File.separator + "Audios"+File.separator;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }


    }

    private void requestPermission() {
        String[] permissions = new String[2];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }




    private void initView() {
        waveview = (RecordWaveView) findViewById(R.id.waveview);
    }

    public void RealWaveRecord(View view) {
        startActivity(new Intent(this, RealTimeWaveRecordActivity.class));
    }

    public void TouchAudioRecord(View view) {
        startActivity(new Intent(this, TouchAudioRecordActivity.class));
    }

    public void WaveAudioRecord(View view) {
        startActivity(new Intent(this, WaveAudioRecordActivity.class));
    }

}
