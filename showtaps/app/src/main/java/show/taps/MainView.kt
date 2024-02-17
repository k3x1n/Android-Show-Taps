package show.taps

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import android.view.Surface.Rotation
import android.view.View
import androidx.core.hardware.display.DisplayManagerCompat
import java.lang.RuntimeException

private const val TAG = "MainView"

class MainView(context: Context) : View(context){

    // 用于画点
    data class Point(var pointerId: Int, var time: Long, var x: Float, var y: Float, var deviceId: Int)

    // 用户绘制按下的状态, 下标是id
    data class PressedPoint(var show: Boolean, var x: Float, var y: Float, var deviceId: Int, var touchRadius: Float)

    // 用于连线, 下标是id (0、1、2、3、4···)：
    data class LastPoint(var head: Boolean, var x: Float, var y: Float)

    /*private */val list = Array(2048) { Point(0, 0L, -9999f, -9999f, 0) }
    /*private */val listPressedPoint = Array(10) { PressedPoint(false, -9999f, -9999f, 0, 0f) } // pointerID : index
    /*private */val lastPoint = Array(10) { LastPoint(false, 0f, 0f) } // pointerID : index

    @Volatile
    var i = 0

    private val circlePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        throw RuntimeException("dispatchTouchEvent?? event=$event")
    }

    private fun Canvas.drawLine(item: Point){
        if(lastPoint[item.pointerId].head){
            if(item.x != -10000f){
                lastPoint[item.pointerId].head = false
                lastPoint[item.pointerId].x = item.x
                lastPoint[item.pointerId].y = item.y
            }

        }else {
            if(item.x != -10000f){
                calculate(item.x, item.y)
                val sx = posRes[0]
                val sy = posRes[1]
                calculate(lastPoint[item.pointerId].x, lastPoint[item.pointerId].y)
                val ex = posRes[0]
                val ey = posRes[1]
                drawLine(sx, sy, ex, ey, circlePaint)

                lastPoint[item.pointerId].head = false
                lastPoint[item.pointerId].x = item.x
                lastPoint[item.pointerId].y = item.y

            }else{
                lastPoint[item.pointerId].head = true

            }

        }

    }

    @Rotation
    var orientation = ROTATION_0

    var posRes = FloatArray(2)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow() called")
        val display = DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)
        if(display == null){
            Log.e(TAG, "onAttachedToWindow: display == null.")
            return
        }
        orientation = display.rotation
        Log.d(TAG, "onAttachedToWindow: orientation = $orientation")
    }

    override fun dispatchConfigurationChanged(newConfig: Configuration?) {
        super.dispatchConfigurationChanged(newConfig)
        Log.d(TAG, "dispatchConfigurationChanged() called with: newConfig = $newConfig")
        if(newConfig != null){
            val display = DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)
            if(display == null){
                Log.e(TAG, "dispatchConfigurationChanged: display == null.")
                return
            }
            orientation = display.rotation
            Log.d(TAG, "dispatchConfigurationChanged: orientation = $orientation")
        }
    }

    private fun calculate(x: Float, y: Float) {
        when(orientation){
            ROTATION_90 -> {
                posRes[0] = y * measuredWidth
                posRes[1] = (1 - x) * measuredHeight
            }
            ROTATION_180 -> {
                posRes[0] = (1 - x) * measuredWidth
                posRes[1] = (1 - y) * measuredHeight
            }
            ROTATION_270 -> {
                posRes[0] = (1 - y) * measuredWidth
                posRes[1] = x * measuredHeight
            }
            else -> {
                posRes[0] = x * measuredWidth
                posRes[1] = y * measuredHeight
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        var requirePostInvalidate = false

        val dp = resources.displayMetrics.density

        val dismissTime = GlobalSettings.dismissTime
        val r = dp * GlobalSettings.touchPointSize / 2

        val colors = GlobalSettings.colorArray

        circlePaint.strokeWidth = dp * GlobalSettings.strokeLine

        try{
            for(lastPointItem in lastPoint){
                lastPointItem.head = true
            }

            for(j in list.indices){
                val index = (j + i + 1) % list.size
                val item = list[index]

                val dt = System.currentTimeMillis() - item.time
                if(dt > dismissTime || dismissTime == 0){
                    continue
                }

                circlePaint.alpha = ((dismissTime - dt) * 255 / dismissTime).toInt()
                circlePaint.color = colors[item.pointerId % colors.size]

                requirePostInvalidate = true

                // draw line :
                if(item.pointerId >= lastPoint.size){
                    Log.d(TAG, "item.pointerId >= lastPoint.size ??? ${item.pointerId}")
                    continue
                }

                if(dismissTime > 0){
                    canvas.drawLine(item)
                }

            }

            circlePaint.strokeWidth = dp * GlobalSettings.strokeCircle

            for(j in listPressedPoint.indices){
                val pressed = listPressedPoint[j]
                if(!pressed.show){
                    continue
                }
                circlePaint.color = colors[j % colors.size]

                if(r != 0f){
                    calculate(pressed.x, pressed.y)
                    canvas.drawCircle(posRes[0], posRes[1], r, circlePaint)
                }
            }

        }catch (e: Exception){
            Log.e(TAG, "onDraw: ", e)
        }

        if(requirePostInvalidate){
            postInvalidateDelayed(16)
        }

    }

}
