package com.xjl.emp3recorder.utils

/**
 * 尝试解耦
 * 总结出 录音和录音动画之间必有的方法进行抽象
 * 当需要绑定动画的时候 使用传接口参数即可
 */
interface RecordBindInterface {
    fun startAnim()
    fun pauseAnum()
    fun stopAnim()
    fun onUpdateAnim(db: Float)
    fun release()
    fun isRunning(): Boolean
    fun onWindowFocusChanged(hasFocus: Boolean)
}
