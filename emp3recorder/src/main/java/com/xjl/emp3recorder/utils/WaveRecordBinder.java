package com.xjl.emp3recorder.utils;

import android.content.Context;
import android.util.Log;

import com.xjl.emp3recorder.mp3record.MP3Recorder;
import com.xjl.emp3recorder.record_wave.RecordWaveView;

import java.io.File;
import java.io.IOException;

public class WaveRecordBinder {

    private static final String TAG = "WaveRecordBinder";

    private RecordWaveView waveview;

    private MP3Recorder mp3Recorder;

    /**
     * 文件存储的地址
     * */
    private String filePath;

    private Context context;

    private File currentRecordFile =null;

    private final String mp3Endcase = ".mp3";

    /**
     * 自动生成地址
     * */
    public WaveRecordBinder(RecordWaveView waveview,String filePath) {
        this.waveview = waveview;
        this.filePath=filePath;
        this.context = this.waveview.getContext();
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        mp3Recorder = new MP3Recorder(file);
        mp3Recorder.setOnRecordListener(recordListener);
        waveview.setRefreshAmplitude(refreshAmplitude);
    }


    MP3Recorder.OnRecordListener recordListener = new MP3Recorder.OnRecordListener() {
        @Override
        public void onStart() {
            Log.e(TAG, "start recording!");
            waveview.startAnim();
        }

        @Override
        public void onStop(File file,long duration) {
            Log.e(TAG, "stop recording file is exist=" + (file != null)+" duration="+duration);
            waveview.stopAnim();
        }

        @Override
        public void onRecording(int mVolumeDb, int mVolume) {

        }


    };

    RecordWaveView.RefreshAmplitude refreshAmplitude = new RecordWaveView.RefreshAmplitude() {
        @Override
        public void refresh() {
            int db=getVolumeDb();
            Log.e(TAG,"getVolumeDb = "+db);
            waveview.setVolume(db);
        }
    };

    public int getVolumeDb() {
        if (mp3Recorder != null) {
            return mp3Recorder.getVolumeDb();
        }
        return 0;
    }

    public int getVolume() {
        if (mp3Recorder != null) {
            return mp3Recorder.getVolume();
        }
        return 0;
    }

    public boolean isRecording() {
        if (mp3Recorder != null) {
            return mp3Recorder.isRecording();
        } else {
            return false;
        }
    }

    /**
     * 绑定相关的生命周期
     * */
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isRecording()) {
            waveview.startAnim();
        }
    }

    public MP3Recorder getMp3Recorder(){
        return mp3Recorder;
    }


    public File start(){
        return start(System.currentTimeMillis()+mp3Endcase);
    }


    public File start(String name){

        if(isRecording()){
            return null;
        }
        String tempFilePath=filePath+name+(name.endsWith(mp3Endcase)?"":mp3Endcase);

        currentRecordFile=new File(tempFilePath);

        if(null==mp3Recorder)
        {
          mp3Recorder=new MP3Recorder(currentRecordFile);
        }else
        {
            mp3Recorder.setFile(currentRecordFile);
        }

        try {
            mp3Recorder.start(-1);
            waveview.startAnim();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return currentRecordFile;
    }



    /**
     * 挂起时时继续录音还是还是保存
     * */
    public void onPause(boolean cancel,boolean delete){
        if(cancel)
        {

        }else
        {


        }
        waveview.pauseAnum();
    }





    public void stop() {
        waveview.stopAnim();
        if (mp3Recorder != null && mp3Recorder.isRecording()){
            mp3Recorder.stop();
            if (mp3Recorder != null){
                mp3Recorder.stop();
            }
        }

    }

    public File getCurrentRecordFile(){
        return currentRecordFile;
    }

}
