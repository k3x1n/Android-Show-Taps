package show.taps.server

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*

@SuppressLint("StaticFieldLeak")
object FloatManager {

    lateinit var view: View

    @SuppressLint("WrongConstant")
    fun init(context: Context, view: View){
        FloatManager.view = view

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val defaultFlags = FLAG_NOT_TOUCH_MODAL or
                FLAG_FULLSCREEN or
                FLAG_LAYOUT_NO_LIMITS or
                FLAG_IGNORE_CHEEK_PRESSES or
                FLAG_NOT_FOCUSABLE or
                FLAG_NOT_TOUCHABLE or
                FLAG_TRANSLUCENT_STATUS or
                FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

        val lp = WindowManager.LayoutParams().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                fitInsetsTypes = 0
            }
            flags = defaultFlags
            type = TYPE_STATUS_BAR_ADDITIONAL
            gravity = Gravity.TOP or Gravity.START
            format = PixelFormat.RGBA_8888
            width = MATCH_PARENT
            height = MATCH_PARENT
            x = 0
            y = 0
        }

        wm.addView(view, lp)

    }

}