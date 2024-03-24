package show.taps.server

import android.os.SystemClock
import android.util.Log
import androidx.annotation.Keep
import show.taps.DevInfo
import show.taps.MainView

private const val TAG = "NativeLib"

@Keep
object NativeLib {

    init {
        System.loadLibrary("taps")
    }

    @JvmStatic
    @Synchronized
    external fun getInputPath(): ArrayList<DevInfo>

    @JvmStatic
    @Synchronized
    external fun start(dev: String?): Boolean

    @JvmStatic
    @Synchronized
    external fun stop()

    @Keep
    @JvmStatic
    fun a(slot: Int, down: Boolean){
        // Log.d(TAG, "a() called with: slot = $slot, down = $down")
        val mainView = FloatManager.view as MainView
        if(mainView.listPressedPoint[slot].pressing == down){
            return
        }
        mainView.listPressedPoint[slot].pressing = down
        if(!down){
            mainView.listPressedPoint[slot].releaseTime = SystemClock.uptimeMillis()
            mainView.list[mainView.i].x = -10000f
        }
    }

    @Keep
    @JvmStatic
    fun b(slot: Int, x: Float, y: Float){
        val mainView = FloatManager.view as MainView

        Log.d(TAG, "b: ${mainView.resources.displayMetrics.widthPixels}")

        val i = mainView.i
        mainView.list[i].x = x
        mainView.list[i].y = y
        mainView.list[i].pointerId = slot
        mainView.list[i].time = SystemClock.uptimeMillis()

        mainView.listPressedPoint[slot].x = mainView.list[i].x
        mainView.listPressedPoint[slot].y = mainView.list[i].y

        if(++mainView.i >= mainView.list.size){
            mainView.i = 0
        }
    }

    @Keep
    @JvmStatic
    fun c(){
        FloatManager.view.postInvalidate()
    }

}