package com.xjl.emp3recorder.mp3record;

import java.io.File;

/**
 * Created by x33664 on 2019/2/21.
 */

public interface OnRecordListener {

    public  void onStart();

    public  void onStop(File file);

}
