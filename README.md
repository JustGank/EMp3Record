#EMp3Record 让 Android 录音播放 更简单

## 一、简介
由于Android原生AudioRecord的录制结果是PCM文件，在其他端不通用，所以需要将录制好的PCM转换成Mp3通用格式。

所以EMp3Record基于Lame的实现实时录音转码输出到文件，同时封装了媒体播放器，帮助开发者播放音频。


## 二、使用 MP3Recorder 录音
### 2.1 初始化

```java
//参数为输出的文件路径
mp3Recorder = new MP3Recorder(currentAudioFile);
mp3Recorder.setOnRecordListener(onRecordListener);
```
其中OnRecordListener为MP3Recorder的内部接口方便录音状态改变时进行回调：

```java
MP3Recorder.OnRecordListener onRecordListener = new MP3Recorder.OnRecordListener() {
        @Override
        public void onStart() {
        }
		
		public void onRecording(int mVolumeDb,int mVolume);
		
        @Override
        public void onStop(File file, long l) {
            handler.sendEmptyMessage(HANDLER_RESET_RECORD_VIEW);
            if (isCancelRecord) {
                file.delete();
            } else {
                if (l < 1000 || file.length() < 1536) {
                    ToastUtils.showMessage(getContext(), getResources().getString(R.string.short_time));
                    file.delete();
                } else {
                    sendMediaMessage(file.getAbsolutePath(), l);
                }
            }
        }
    };
```

**public void onRecording(int mVolumeDb,int mVolume)** 返回的是录音是的音量大小，方便动画的实现。

**public void onStop(File file, long l)** 返回的参数是录音文件，和录音时长。

## 2.2 开始和结束录音
开始录音：**MP3Recorder#public void start(final long maxDuration)** 
其中设置参数为最大录音时长。

参考样例：

```java
currentAudioFile = new File(ChatFileManager.cacheDirPathAudio + File.separator + System.currentTimeMillis() + ".mp3");
mp3Recorder.setFile(currentAudioFile);
mp3Recorder.start(60 * 1000);
```

**结束录音：mp3Recorder.stop();**

## 三、使用 MediaPlayerHepler 播放音频
### 3.1 初始化

```java
mediaPlayerHepler = new MediaPlayerHepler(getActivity());
mediaPlayerHepler.setOnCompleteListener(completionListener);
```

### 3.2 注册播放完成时的回调监听  

```java
MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mediaPlayerHepler != null) {
                mediaPlayerHepler.stop();
            }
        }
    };
```
### 3.3 播放音频

```java
MediaPlayerHepler#start(String filePath);
```

### 3.4 暂停播放
```java
MediaPlayerHepler#pause();
```

### 3.5 停止播放
```java
MediaPlayerHepler#stop();
```
