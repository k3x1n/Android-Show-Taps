package show.taps.server

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import show.taps.BuildConfig
import show.taps.DevInfo
import show.taps.KernelInterface
import show.taps.MainView
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private const val TAG = "KernelService"

class KernelService(context: Context) : KernelInterface.Stub() {

    private val handler = Handler(Looper.getMainLooper())

    private var lastActive = SystemClock.uptimeMillis()

    private var isRunning = false

    @SuppressLint("PrivateApi")
    private fun fixVivo(){
        // VIVO android 10
        try{
            val field = Class.forName("android.view.VivoViewRootImplImpl")
                .getDeclaredField("SUPPORT_PCSHARE")
            field.isAccessible = true
            field.set(null, false)
        }catch (e: Exception){
            Log.e(TAG, "fixVivo: ${e.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun fixLge(){
        // LG android 11
        try{
            val field = Class.forName("com.lge.os.Build\$VERSION")
                .getDeclaredField("IS_OS_UPGRADED")
            field.isAccessible = true
            field.set(null, false)
        }catch (e: Exception){
            Log.e(TAG, "fixLge11: ${e.message}")
        }
    }

    @SuppressLint("SoonBlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi")
    private fun fixCommon(){
        try{
            val holderClazz = Class.forName("android.provider.Settings\$ContentProviderHolder")

            val contentProviderField = holderClazz.getDeclaredField("mContentProvider")
            contentProviderField.isAccessible = true

            val list = listOf(Settings.Secure::class.java, Settings.System::class.java, Settings.Global::class.java)
            for(clazz in list){
                try {
                    val sProviderHolderField = clazz.getDeclaredField("sProviderHolder")
                    sProviderHolderField.isAccessible = true
                    val holder = sProviderHolderField.get(clazz) // Settings$ContentProviderHolder
                    contentProviderField.set(holder, UnknownContentProvider())

                }catch (e: Exception){
                    Log.e(TAG, "fixCommon: clazz=$clazz ${e.message}")
                }
            }

        }catch (e: Exception){
            Log.e(TAG, "fixCommon: ", e)
        }

    }

    init{
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try{
                Log.e(TAG, "exception: ", e)
                val path = File("/data/local/tmp/show.taps/")
                path.mkdirs()
                val file = File(path, "crash_${System.currentTimeMillis()}.txt")
                file.createNewFile()
                val info = "pid=${Process.myPid()},uid=${Process.myUid()},t_name=${t.name}"
                file.writeText(info + "\n" + Log.getStackTraceString(e))
            }catch (e: Exception){
                Log.e(TAG, "loge: ", e)
            }finally {
                exitProcess(0)
            }
        }

        Log.d(TAG, "init!")
        val pidPath = File("/data/local/tmp/show.taps/server_pid/")
        if(!pidPath.exists()){
            pidPath.mkdirs()
        }else{
            if(pidPath.isFile){
                pidPath.delete()
                pidPath.mkdirs()
            }
            pidPath.listFiles()?.forEach {
                runCatching {
                    it.delete()
                }
            }
        }
        val liveFile = File(pidPath, Process.myPid().toString())
        liveFile.createNewFile()

        fixCommon()
        fixVivo()
        fixLge()

        FloatManager.init(context, MainView(context))

        thread(name = "avoid_forgetting_exit"){
            while(true){
                SystemClock.sleep(5000)

                if(!isRunning && SystemClock.uptimeMillis() - lastActive >= 120 * 1000){
                    Log.d(TAG, "isRunning == false!! timeout!!")
                    exitProcess(0)
                }

                if(!liveFile.exists()){
                    Log.d(TAG, "avoid_multi_process: ${liveFile.absolutePath}")
                    exitProcess(0)
                }

                @SuppressLint("SdCardPath")
                if(!File("/data/data/${BuildConfig.APPLICATION_ID}/").exists()){
                    Log.d(TAG, "uninstall: ${liveFile.absolutePath}")
                    exitProcess(0)
                }
            }
        }
    }

    private fun getInputPath(): Array<DevInfo> {
        lastActive = SystemClock.uptimeMillis()
        return NativeLib.getInputPath().toTypedArray()
    }

    @Synchronized
    override fun start(dev: String?, colorArray: IntArray, touchPointSize: Int, dismissTime: Int,
                       circleStroke: Int, lineStroke: Int, colorAplha: Int): Boolean {
        val inputPath = getInputPath()
        if(inputPath.isEmpty()) {
            Log.e(TAG, "start: inputPath isEmpty.")
            return false
        }
        if(dev != null){
            inputPath.forEach{
                if(it.name == dev) {
                    it.weight += 100000
                }
            }
        }
        var path = inputPath[0].path
        var weight = inputPath[0].weight
        inputPath.forEach {
            if(it.weight > weight){
                weight = it.weight
                path = it.path
            }
        }
        lastActive = SystemClock.uptimeMillis()
        for(i in 0 until GlobalSettings.COLOR_ARRAY_LENGTH){
            GlobalSettings.colorArray[i] = colorArray[i]
        }
        GlobalSettings.touchPointSize = touchPointSize
        GlobalSettings.pathFadeTime = dismissTime
        GlobalSettings.strokeCircle = circleStroke
        GlobalSettings.strokeLine = lineStroke
        handler.post{
            FloatManager.view.alpha = colorAplha / 100f
        }
        if(NativeLib.start(path)){
            isRunning = true
            return true
        }
        return false
    }

    override fun isRunning() : Boolean{
        lastActive = SystemClock.uptimeMillis()
        return isRunning
    }

    @Synchronized
    override fun stop() {
        lastActive = SystemClock.uptimeMillis()
        NativeLib.stop()
        isRunning = false
    }

    override fun updateInfo(touchPointSize: Int, dismissTime: Int,
                            circleStroke: Int, lineStroke: Int, colorAplha: Int) {
        lastActive = SystemClock.uptimeMillis()
        GlobalSettings.touchPointSize = touchPointSize
        GlobalSettings.pathFadeTime = dismissTime
        GlobalSettings.strokeCircle = circleStroke
        GlobalSettings.strokeLine = lineStroke
        handler.post{
            FloatManager.view.alpha = colorAplha / 100f
        }
    }

    override fun updateColors(colorArray: IntArray) {
        lastActive = SystemClock.uptimeMillis()
        for(i in 0 until GlobalSettings.COLOR_ARRAY_LENGTH){
            GlobalSettings.colorArray[i] = colorArray[i]
        }
    }

}