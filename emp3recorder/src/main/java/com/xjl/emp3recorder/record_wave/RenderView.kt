package com.xjl.emp3recorder.record_wave

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.xjl.emp3recorder.utils.RecordBindInterface

/**
 * Created by x33664 on 2019/2/18.
 */
abstract class RenderView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, RecordBindInterface {

    private var isAutoStartAnim = false
    private val surfaceLock = Any()
    private var renderThread: RenderThread? = null
    private val SLEEP_TIME: Long = 20

    init {
        holder.addCallback(this)
    }

    private inner class RenderThread(private val surfaceHolder: SurfaceHolder) :
        Thread("RenderThread") {
        var running = false
        var destoryed = false
        var isPause = false
        override fun run() {
            val startAt = System.currentTimeMillis()
            while (true) {
                synchronized(surfaceLock) {
                    if (destoryed) {
                        return
                    }

                    //这里并没有真正的结束Thread，防止部分手机连续调用同一Thread出错
                    while (isPause) {
                        try {
                            (surfaceLock as Object).wait()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    if (running) {
                        surfaceHolder.lockCanvas().let { canvas ->
                            onRender(canvas, System.currentTimeMillis() - startAt) //这里做真正绘制的事情
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        }
                    }
                    try {
                        sleep(SLEEP_TIME)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun setRun(isRun: Boolean) {
            running = isRun
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderThread = RenderThread(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(surfaceLock) {
            //这里需要加锁，否则doDraw中有可能会crash
            renderThread?.apply {
                setRun(false)
                destoryed = true
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && isAutoStartAnim) {
            startAnim()
        }
    }

    /**
     * 渲染surfaceView的回调方法。
     *
     * @param canvas 画布
     */
    abstract fun onRender(canvas: Canvas, millisPassed: Long)

    override fun startAnim() {
        renderThread?.apply {
            if (!running) {
                setRun(true)
                if (state == Thread.State.NEW) {
                    start()
                }
            }
        }
    }

    /**
     * 解锁暂停，继续执行绘制任务
     * 默认当Resume时不自动启动动画
     */
    fun onResume() {
        synchronized(surfaceLock) {
            renderThread?.apply {
                isPause = false
                (surfaceLock as Object).notifyAll()
            }
        }
    }

    /**
     * 解锁暂停，继续执行绘制任务
     * @param isAutoStartAnim 是否当Resume时自动启动动画
     */
    fun onResume(isAutoStartAnim: Boolean) {
        synchronized(surfaceLock) {
            this.isAutoStartAnim = isAutoStartAnim
            if (this.isAutoStartAnim) {
                renderThread?.apply {
                    isPause = false
                    (surfaceLock as Object).notifyAll()
                }
            }
        }
    }

    //假暂停，并没有结束Thread
    override fun pauseAnum() {
        synchronized(surfaceLock) {
            renderThread?.isPause = true
        }
    }

    override fun stopAnim() {
        renderThread?.apply {
            if (running) {
                setRun(false)
                interrupt()
            }
        }
    }

     override fun isRunning(): Boolean {
        return renderThread?.running?:false
    }

    override fun onUpdateAnim(db: Float) {}
    override fun release() {}
}
