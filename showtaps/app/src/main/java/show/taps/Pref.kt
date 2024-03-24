package show.taps

import android.content.Context
import show.taps.server.GlobalSettings

const val keySize = "PREF_KEY_TOUCH_POINT_SIZE"

const val keyTime = "PREF_KEY_TOUCH_PATH_DISAPPEARANCE_TIME"

const val keyStrokeCircle = "PREF_KEY_STROKE_CIRCLE"

const val keyStrokeLine = "PREF_KEY_STROKE_LINE"

const val keyAlpha = "PREF_KEY_COLOR_ALPHA"

const val keyColorPrefix = "PREF_KEY_COLOR_"

const val touchEventDevName = "TOUCH_EVENT_DEV_NAME"

fun pref() = App.instance.getSharedPreferences("Settings", Context.MODE_PRIVATE)!!

fun getTouchPointSize() = pref().getInt(keySize, 80)

fun getTouchPathDisappearanceTime() = pref().getInt(keyTime, 200)

fun getStrokeWidthCircle() = pref().getInt(keyStrokeCircle, 5)

fun getStrokeWidthLine() = pref().getInt(keyStrokeLine, 5)

fun getAlpha() = pref().getInt(keyAlpha, 100)

fun getLastDevName(): String? = pref().getString(touchEventDevName, null)

fun putLastDevName(value: String?) {
    pref().edit().putString(touchEventDevName, value).apply()
}

/** 长度必须是 [GlobalSettings.COLOR_ARRAY_LENGTH] */
fun getColorList(): IntArray {
    val pref = pref()
    val res = IntArray(GlobalSettings.COLOR_ARRAY_LENGTH)
    for(i in 0 until GlobalSettings.COLOR_ARRAY_LENGTH){
        res[i] = pref.getInt("$keyColorPrefix$i", defaultColors[i])
    }
    return res
}

/** 长度必须是 [GlobalSettings.COLOR_ARRAY_LENGTH] */
private val defaultColors = intArrayOf(
    0xFF8B00FF.toInt(), // 紫色
    0xFF00FF00.toInt(), // 绿色
    0xFFFFA500.toInt(), // 橙色
    0xFFFF0000.toInt(), // 红色
    0xFF0000FF.toInt(), // 蓝色
    0xFFFFFF00.toInt(), // 黄色
    0xFF00BFFF.toInt(), // 淡蓝色
    0xFFFF00FF.toInt(), // 粉色
    0xFFFF6666.toInt(), // 浅红色
    0xFFFFB266.toInt()  // 浅橙色
)
