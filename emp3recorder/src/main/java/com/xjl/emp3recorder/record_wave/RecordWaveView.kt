package com.xjl.emp3recorder.record_wave

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Xfermode
import android.util.AttributeSet
import android.util.SparseArray
import androidx.core.content.ContextCompat
import com.xjl.emp3record.R
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Created by x33664 on 2019/2/18.
 * 波浪形的Wave
 *
 */
class RecordWaveView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RenderView(context, attrs, defStyleAttr) {
    //采样点的数量，越高越精细，但是高于一定限度肉眼很难分辨，越高绘制效率越低
    private var SAMPLING_SIZE = 64

    //控制向右偏移速度，越小偏移速度越快
    private var OFFSET_SPEED = 500f

    //圆球的速度，越小速度越快
    private var CIRCLE_SPEED = 150f

    //小球默认半径
    private var DEFAULT_CIRCLE_RADIUS = 0f
    private val paint = Paint()

    init {
        //防抖动
        paint.isDither = true
        //抗锯齿，降低分辨率，提高绘制效率
        paint.isAntiAlias = true
    }

    //最上方的线
    private val firstPath = Path()

    //中间的振幅很小的线
    private val centerPath = Path()

    //最下方的线
    private val secondPath = Path()

    //采样点X坐标
    private var samplingX: FloatArray? = null

    //采样点位置映射到[-2,2]之间
    private var mapX: FloatArray? = null

    //画布宽高
    private var width = 0
    private var height = 0

    //画布中心的高度
    private var centerHeight = 0

    //振幅
    private var amplitude = 0

    /**
     * 波峰和两条路径交叉点的记录，包括起点和终点，用于绘制渐变。
     * 其数量范围为7~9个，这里size取9。
     * 每个元素都是一个float[2]，用于保存xy值
     */
    private val crestAndCrossPints = arrayOfNulls<FloatArray>(10)

    init { //直接分配内存
        for (i in 0..8) {
            crestAndCrossPints[i] = FloatArray(2)
        }
    }

    //用于处理矩形的rectF
    private val rectF = RectF()

    /**
     * 图像回合模式机制，它能够控制绘制图形与之前已经存在的图形的混合交叠模式
     * 这里通过SRC_IN模式将已经绘制好的波形图与渐变矩形取交集，绘制出渐变色
     */
    private val xfermode: Xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    //背景色
    private var backGroundColor = 0

    //中间线的颜色
    private var centerPathColor = 0

    //第一条曲线的颜色
    private var firstPathColor = 0

    //第二条曲线的颜色
    private var secondPathColor = 0

    //是否显示小球
    private var isShowBalls = false

    //存储衰减系数
    private val recessionFuncs = SparseArray<Double>()
    private fun init(attrs: AttributeSet?) {
        val t = context.obtainStyledAttributes(attrs, R.styleable.RecordWaveView)
        backGroundColor = t.getColor(
            R.styleable.RecordWaveView_backgroundColor,
            ContextCompat.getColor(context, R.color.backgroundColor)
        )
        firstPathColor = t.getColor(
            R.styleable.RecordWaveView_firstPathColor,
            ContextCompat.getColor(context, R.color.firstPathColor)
        )
        secondPathColor = t.getColor(
            R.styleable.RecordWaveView_secondPathColor,
            ContextCompat.getColor(context, R.color.secondPathColor)
        )
        centerPathColor = t.getColor(
            R.styleable.RecordWaveView_centerPathColor,
            ContextCompat.getColor(context, R.color.centerPathColor)
        )
        isShowBalls = t.getBoolean(R.styleable.RecordWaveView_showBalls, true)
        amplitude = t.getDimensionPixelSize(R.styleable.RecordWaveView_amplitude, 0)
        SAMPLING_SIZE = t.getInt(R.styleable.RecordWaveView_ballSpeed, 64)
        OFFSET_SPEED = t.getFloat(R.styleable.RecordWaveView_moveSpeed, 500f)
        CIRCLE_SPEED = t.getFloat(R.styleable.RecordWaveView_ballSpeed, 150f)
        DEFAULT_CIRCLE_RADIUS = dip2px(3f).toFloat()
        t.recycle()
    }

    override fun onRender(canvas: Canvas, millisPassed: Long) {
        if (null == samplingX) {
            initDraw(canvas)
        }

        refreshAmplitude?.refresh()

        //绘制背景
        canvas.drawColor(backGroundColor)

        //重置所有path并移动到起点
        firstPath.rewind()
        centerPath.rewind()
        secondPath.rewind()
        firstPath.moveTo(0f, centerHeight.toFloat())
        centerPath.moveTo(0f, centerHeight.toFloat())
        secondPath.moveTo(0f, centerHeight.toFloat())

        //当前时间的偏移量，通过该偏移量使每次绘制向右偏移，从而让曲线动起来
        val offset = millisPassed / OFFSET_SPEED


        //波形函数的值，包括上一点，当前点，下一点
        var lastV: Float
        var curV = 0f
        var nextV = (amplitude * calcValue(mapX!![0], offset)).toFloat()
        //波形函数的绝对值，用于筛选波峰和交错点
        var absLastV: Float
        var absCurV: Float
        var absNextV: Float
        //上次的筛选点是波峰还是交错点
        var lastIsCrest = false
        //筛选出的波峰和交错点的数量，包括起点和终点
        var crestAndCrossCount = 0
        var x: Float
        var xy: FloatArray?
        for (i in 0..SAMPLING_SIZE) {
            //计算采样点的位置
            x = samplingX!![i]
            lastV = curV
            curV = nextV
            //计算下一采样点的位置，并判断是否到终点
            nextV =
                if (i < SAMPLING_SIZE) (amplitude * calcValue(mapX!![i + 1], offset)).toFloat() else 0f

            //连接路径
            firstPath.lineTo(x, centerHeight + curV)
            secondPath.lineTo(x, centerHeight - curV)
            //中间曲线的振幅是上下曲线的1/5
            centerPath.lineTo(x, centerHeight - curV / 5f)

            //记录极值点
            absLastV = abs(lastV.toDouble()).toFloat()
            absCurV = abs(curV.toDouble()).toFloat()
            absNextV = abs(nextV.toDouble()).toFloat()
            if (i == 0 || i == SAMPLING_SIZE || lastIsCrest && absCurV < absLastV && absCurV < absNextV /*上一个点为波峰，且该点是极小值点*/) {
                //交叉点
                xy = crestAndCrossPints[crestAndCrossCount++]
                xy!![0] = x
                xy[1] = 0f
                lastIsCrest = false
            } else if (!lastIsCrest && absCurV > absLastV && absCurV > absNextV) { /*上一点是交叉点，且该点极大值*/
                //极大值点
                xy = crestAndCrossPints[crestAndCrossCount++]
                xy!![0] = x
                xy[1] = curV
                lastIsCrest = true
            }
        }

        //连接所有路径到终点
        firstPath.lineTo(width.toFloat(), centerHeight.toFloat())
        secondPath.lineTo(width.toFloat(), centerHeight.toFloat())
        centerPath.lineTo(width.toFloat(), centerHeight.toFloat())

        //记录layer,将图层进行离屏缓存
        val saveCount =
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null, Canvas.ALL_SAVE_FLAG)

        //填充上下两条正弦函数，为下一步混合交叠做准备
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.strokeWidth = 1f
        canvas.drawPath(firstPath, paint)
        canvas.drawPath(secondPath, paint)
        paint.color = firstPathColor
        paint.style = Paint.Style.FILL
        paint.setXfermode(xfermode)
        var startX: Float
        var crestY: Float
        var endX: Float
        //根据上面计算的峰顶和交叉点的位置，绘制矩形
        var i = 2
        while (i < crestAndCrossCount) {

            //每隔两个点绘制一个矩形
            startX = crestAndCrossPints[i - 2]!![0]
            crestY = crestAndCrossPints[i - 1]!![1]
            endX = crestAndCrossPints[i]!![0]

            //设置渐变矩形区域
            paint.setShader(
                LinearGradient(
                    0f, centerHeight + crestY, 0f,
                    centerHeight - crestY, firstPathColor, secondPathColor,
                    Shader.TileMode.CLAMP
                )
            )
            rectF[startX, centerHeight + crestY, endX] = centerHeight - crestY
            canvas.drawRect(rectF, paint)
            i += 2
        }

        //释放画笔资源
        paint.setShader(null)
        paint.setXfermode(null)

        //叠加layer，因为使用了SRC_IN的模式所以只会保留波形渐变重合的地方
        canvas.restoreToCount(saveCount)

        //绘制上弦线
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        paint.color = firstPathColor
        canvas.drawPath(firstPath, paint)

        //绘制下弦线
        paint.color = secondPathColor
        canvas.drawPath(secondPath, paint)

        //绘制中间线
        paint.color = centerPathColor
        canvas.drawPath(centerPath, paint)
        if (isShowBalls) {
            val circleOffset = millisPassed / CIRCLE_SPEED
            drawCircleBalls(circleOffset, canvas)
        }
    }

    //根据分贝设置不同的振幅
    private fun getAmplitude(db: Int): Int {
        return if (db <= 40) {
            width shr 4
        } else {
            width shr 3
        }
    }

    //初始化绘制参数
    private fun initDraw(canvas: Canvas) {
        width = canvas.width
        height = canvas.height
        centerHeight = height shr 1
        //振幅为宽度的1/8
        //如果未设置振幅高度，则使用默认高度
        if (amplitude == 0) {
            amplitude = width shr 3
        }

        //初始化采样点及映射

        //这里因为包括起点和终点，所以需要+1
        samplingX = FloatArray(SAMPLING_SIZE + 1)
        mapX = FloatArray(SAMPLING_SIZE + 1)
        //确定采样点之间的间距
        val gap = width / SAMPLING_SIZE.toFloat()
        //采样点的位置
        var x: Float
        for (i in 0..SAMPLING_SIZE) {
            x = i * gap
            samplingX!![i] = x
            //将采样点映射到[-2，2]
            mapX!![i] = x / width.toFloat() * 4 - 2
        }
    }

    /**
     * 计算波形函数中x对应的y值
     * 使用稀疏矩阵进行暂存计算好的衰减系数值，下次使用时直接查找，减少计算量
     *
     * @param mapX   换算到[-2,2]之间的x值
     * @param offset 偏移量
     * @return
     */
    private fun calcValue(mapX: Float, offset: Float): Double {
        var tempOffset = offset
        val keyX = (mapX * 1000).toInt()
        tempOffset %= 2f
        val sinFunc = sin(0.75 * Math.PI * mapX - tempOffset * Math.PI)
        val recessionFunc: Double
        if (recessionFuncs.indexOfKey(keyX) >= 0) {
            recessionFunc = recessionFuncs[keyX]
        } else {
            recessionFunc = (4 / (4 + mapX.pow(4.0f))).pow(2.5f).toDouble()
            recessionFuncs.put(keyX, recessionFunc)
        }
        return sinFunc * recessionFunc
    }

    //绘制自由运动的小球
    private fun drawCircleBalls(speed: Float, canvas: Canvas?) {
        //从左到右依次绘制
        paint.color = firstPathColor
        paint.style = Paint.Style.FILL
        var x: Float = width / 6f + 40 * sin(0.45 * speed - CIRCLE_SPEED * Math.PI).toFloat()
        var y: Float = centerHeight + 50 * sin(speed.toDouble()).toFloat()
        canvas!!.drawCircle(x, y, DEFAULT_CIRCLE_RADIUS, paint)
        paint.color = secondPathColor
        x = 2 * width / 6f + 20 * sin(speed.toDouble()).toFloat()
        y = centerHeight + sin(speed.toDouble()).toFloat()
        canvas.drawCircle(x, y, DEFAULT_CIRCLE_RADIUS * 0.8f, paint)
        paint.color = secondPathColor
        paint.alpha = 60 + Random().nextInt(40)
        x = 2.5f * width / 6f + 40 * sin(0.35 * speed + CIRCLE_SPEED * Math.PI)
            .toFloat()
        y = centerHeight + 40 * sin(speed.toDouble()).toFloat()
        canvas.drawCircle(x, y, DEFAULT_CIRCLE_RADIUS, paint)
        paint.color = firstPathColor
        x = 3f * width / 6f + cos(speed.toDouble()).toFloat()
        y = centerHeight + 40 * sin((0.6f * speed).toDouble()).toFloat()
        canvas.drawCircle(x, y, DEFAULT_CIRCLE_RADIUS * 0.7f, paint)
        paint.color = secondPathColor
        x = 4 * width / 6f + 70 * sin(speed.toDouble()).toFloat()
        y = centerHeight + 10 * sin(speed.toDouble()).toFloat()
        canvas.drawCircle(x, y, DEFAULT_CIRCLE_RADIUS * 0.5f, paint)
        paint.color = firstPathColor
        x = 5.2f * width / 6f + 30 * sin(0.21 * speed + CIRCLE_SPEED * Math.PI)
            .toFloat()
        y = centerHeight + 10 * cos(speed.toDouble()).toFloat()
        canvas.drawCircle(x, y, DEFAULT_CIRCLE_RADIUS * 0.75f, paint)
        paint.color = secondPathColor
        x = 5.5f * width / 6f + 60 * sin(0.15 * speed - CIRCLE_SPEED * Math.PI)
            .toFloat()
        y = centerHeight + 50 * sin(speed.toDouble()).toFloat()
        canvas.drawCircle(x, y, DEFAULT_CIRCLE_RADIUS * 0.7f, paint)
    }

    private fun dip2px(dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    //设置音量分贝
    fun setVolume(db: Int) {
        amplitude = getAmplitude(db)
    }

    private var refreshAmplitude: RefreshAmplitude? = null

    init {
        init(attrs)
    }

    fun setRefreshAmplitude(refreshAmplitude: RefreshAmplitude?) {
        this.refreshAmplitude = refreshAmplitude
    }

    interface RefreshAmplitude {
        fun refresh()
    }
}
