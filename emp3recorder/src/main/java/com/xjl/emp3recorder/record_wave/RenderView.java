package com.xjl.emp3recorder.record_wave;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.xjl.emp3recorder.utils.RecordBindInterface;

import java.util.List;

/**
 * Created by x33664 on 2019/2/18.
 */

public class RenderView extends SurfaceView implements SurfaceHolder.Callback , RecordBindInterface {

    private boolean isAutoStartAnim = false;

    public RenderView(Context context) {
        this(context,null);

    }

    public RenderView(Context context, AttributeSet attrs) {
        this(context, attrs,0);

    }

    public RenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    private final Object surfaceLock = new Object();
    private RenderThread renderThread;

    private  class RenderThread extends Thread{
        private static final long SLEEP_TIME = 20;

        private SurfaceHolder surfaceHolder;
        private boolean running = false;
        private boolean destoryed = false;
        private boolean isPause = false;

        public RenderThread(SurfaceHolder holder) {
            super("RenderThread");
            surfaceHolder = holder;
        }

        @Override
        public void run() {
            long startAt = System.currentTimeMillis();
            while (true) {
                synchronized (surfaceLock) {
                    if (destoryed){
                        return;
                    }

                    //这里并没有真正的结束Thread，防止部分手机连续调用同一Thread出错
                    while (isPause){
                        try {
                            surfaceLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (running) {
                        Canvas canvas = surfaceHolder.lockCanvas();
                        if (canvas != null) {
                            render(canvas, System.currentTimeMillis() - startAt);  //这里做真正绘制的事情
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setRun(boolean isRun) {
            this.running = isRun;
        }

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderer = onCreateRenderer();
        if (renderer != null && renderer.isEmpty()) {
            throw new IllegalStateException();
        }

        renderThread = new RenderThread(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (surfaceLock) {  //这里需要加锁，否则doDraw中有可能会crash
            renderThread.setRun(false);
            renderThread.destoryed = true;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isAutoStartAnim){
            startAnim();
        }
    }

     /*绘图*/

    public interface IRenderer {
        void onRender(Canvas canvas, long millisPassed);
    }

    private List<IRenderer> renderer;

    protected List<IRenderer> onCreateRenderer() {
        return null;
    }

    private void render(Canvas canvas, long millisPassed) {
        if (renderer != null) {
            for (int i = 0, size = renderer.size(); i < size; i++) {
                renderer.get(i).onRender(canvas, millisPassed);
            }
        } else {
            onRender(canvas, millisPassed);
        }
    }

    /**
     * 渲染surfaceView的回调方法。
     *
     * @param canvas 画布
     */
    protected void onRender(Canvas canvas, long millisPassed) {
    }

    @Override
    public void startAnim(){

        if (renderThread != null && !renderThread.running) {
            renderThread.setRun(true);
            try {
                if (renderThread.getState() == Thread.State.NEW) {
                    renderThread.start();
                }

            }catch (RuntimeException e){
                e.printStackTrace();
            }

        }
    }


    /**
     * 解锁暂停，继续执行绘制任务
     * 默认当Resume时不自动启动动画
     */
    public void onResume(){
        synchronized (surfaceLock){
            if (renderThread != null) {
                renderThread.isPause = false;
                surfaceLock.notifyAll();
            }
        }
    }

    /**
     * 解锁暂停，继续执行绘制任务
     * @param isAutoStartAnim 是否当Resume时自动启动动画
     */
    public void onResume(boolean isAutoStartAnim){
        synchronized (surfaceLock){
            this.isAutoStartAnim = isAutoStartAnim;
            if (renderThread != null) {
                renderThread.isPause = false;
                surfaceLock.notifyAll();
            }
        }
    }




    //假暂停，并没有结束Thread
    @Override
    public void pauseAnum(){
        synchronized (surfaceLock){
            if (renderThread != null) {
                renderThread.isPause = true;
            }
        }
    }

    @Override
    public void stopAnim(){
        if (renderThread != null && renderThread.running) {
            renderThread.setRun(false);
            renderThread.interrupt();
        }
    }

    @Override
    public boolean isRunning(){
        if (renderThread != null) {
            return renderThread.running;
        }
        return false;
    }

    @Override
    public void onUpdateAnim(float db) {

    }

    @Override
    public void release( ) {

    }

}
