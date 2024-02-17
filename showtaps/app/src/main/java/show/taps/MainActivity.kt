package show.taps

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import show.taps.databinding.ActivityMainBinding
import show.taps.databinding.ActivityMainCard1Binding
import show.taps.databinding.ViewColorItemBinding
import java.util.Arrays
import kotlin.system.exitProcess

private const val TAG = "MainActivity"

private const val REQ_CODE_SHIZUKU = 1001

private const val PKG_SHIZUKU = "moe.shizuku.privileged.api"

/**
 * adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
 */
class MainActivity : AppCompatActivity(), Shizuku.OnRequestPermissionResultListener {

    private lateinit var binding: ActivityMainBinding

    private var mKernelInterface : KernelInterface? = null

    private fun ActivityMainBinding.initPrefPanel(){

        fun setTouchPointSize(i : Int) = pref().edit().putInt(keySize, i).apply()

        fun setTouchPathDisappearanceTime(i : Int) = pref().edit().putInt(keyTime, i).apply()

        fun setStrokeCircle(i : Int) = pref().edit().putInt(keyStrokeCircle, i).apply()

        fun setStrokeLine(i : Int) = pref().edit().putInt(keyStrokeLine, i).apply()

        fun setAlpha(i : Int) = pref().edit().putInt(keyAlpha, i).apply()

        fun updateRemoteInfo(){
            try{
                val kernelInterface = mKernelInterface
                if(kernelInterface?.asBinder()?.isBinderAlive == true){
                    kernelInterface.updateInfo(
                        getTouchPointSize(), getTouchPathDisappearanceTime(),
                        getStrokeWidthCircle(), getStrokeWidthLine(), getAlpha())
                }
            }catch (e: Exception){
                Log.e(TAG, "onStopTrackingTouch: ", e)
            }
        }

        layout2.tvTouchDisappearanceTime.text = getString(R.string.tv_touch_line_time, 0)
        layout2.seekbarFadeTime.setOnSeekBarChangeListener(OnSeekBarChangeListener{progress, fromUser->
            layout2.tvTouchDisappearanceTime.text = getString(
                R.string.tv_touch_line_time, progress)
            if(fromUser){
                setTouchPathDisappearanceTime(progress)
                updateRemoteInfo()
            }
        })

        layout2.tvTouchPointSize.text = getString(R.string.tv_touch_point_size, 0)
        layout2.seekbarStokeSize.setOnSeekBarChangeListener(OnSeekBarChangeListener{progress, fromUser->
            layout2.tvTouchPointSize.text = getString(R.string.tv_touch_point_size, progress)
            if(fromUser){
                setTouchPointSize(progress)
                updateRemoteInfo()
            }
        })

        layout2.tvCircleStroke.text = getString(R.string.tv_touch_point_stroke, 0)
        layout2.seekbarCircleStroke.setOnSeekBarChangeListener(OnSeekBarChangeListener{progress, fromUser->
            layout2.tvCircleStroke.text = getString(R.string.tv_touch_point_stroke, progress)
            if(fromUser){
                setStrokeCircle(progress)
                updateRemoteInfo()
            }
        })

        layout2.tvLineStroke.text = getString(R.string.tv_touch_line_stroke, 0)
        layout2.seekbarLineStroke.setOnSeekBarChangeListener(OnSeekBarChangeListener{progress, fromUser->
            layout2.tvLineStroke.text = getString(R.string.tv_touch_line_stroke, progress)
            if(fromUser){
                setStrokeLine(progress)
                updateRemoteInfo()
            }
        })

        layout2.tvColorAlpha.text = getString(R.string.tv_touch_alpha, 30)
        layout2.seekbarColorAlpha.setOnSeekBarChangeListener(OnSeekBarChangeListener{progress, fromUser->
            layout2.tvColorAlpha.text = getString(R.string.tv_touch_alpha, progress)
            if(fromUser){
                setAlpha(progress)
                updateRemoteInfo()
            }
        })

        layout2.seekbarFadeTime.progress = getTouchPathDisappearanceTime()

        layout2.seekbarStokeSize.progress = getTouchPointSize()

        layout2.seekbarCircleStroke.progress = getStrokeWidthCircle()

        layout2.seekbarLineStroke.progress = getStrokeWidthLine()

        layout2.seekbarColorAlpha.progress = getAlpha()

    }

    private var stop = false

    override fun onStop() {
        super.onStop()
        stop = true
    }

    private fun ActivityMainBinding.initLink(){
        layout3.linkPlay.setOnClickListener {
            openGooglePlayStore()
        }
        layout3.linkPlay.setOnLongClickListener {
            copy("https://play.google.com/store/apps/details?id=${packageName}")
            Toast.makeText(this@MainActivity,
                getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            true
        }

        layout3.linkTube.setOnClickListener {
            openWebBrowser("https://www.youtube.com/@k3x1n-dev")
        }
        layout3.linkTube.setOnLongClickListener {
            copy("https://www.youtube.com/@k3x1n-dev")
            Toast.makeText(this@MainActivity,
                getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            true
        }


        layout3.linkGithub.setOnClickListener {
            openWebBrowser("https://github.com/k3x1n/Android-Show-Taps")
        }
        layout3.linkGithub.setOnLongClickListener {
            copy("https://github.com/k3x1n/Android-Show-Taps")
            Toast.makeText(this@MainActivity,
                getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            true
        }

        @Suppress("SetTextI18n")
        layout3.tvVersion.text = "version ${BuildConfig.VERSION_NAME}"

    }

    /** 长度必须是 [GlobalSettings.COLOR_ARRAY_LENGTH] */
    private lateinit var colorList : IntArray

    private fun ActivityMainBinding.initColor(){

        fun saveColor(i : Int, color: Int) = pref().edit().putInt("$keyColorPrefix$i", color).apply()

        fun updateRemoteColorInfo(){
            try{
                val kernelInterface = mKernelInterface
                if(kernelInterface?.asBinder()?.isBinderAlive == true){
                    kernelInterface.updateColors(colorList)
                }
            }catch (e: Exception){
                Log.e(TAG, "updateRemoteColorInfo: ", e)
            }
        }

        colorList = getColorList()
        for(i in 0 until GlobalSettings.COLOR_ARRAY_LENGTH){
            val binding = ViewColorItemBinding.inflate(layoutInflater, layout2.colorList, false)
            binding.imgColor.setImageDrawable(ColorDrawable(colorList[i]))
            binding.root.setOnClickListener {
                ColorPickerDialog.Builder(this@MainActivity)
                    .setPositiveButton("OK", ColorEnvelopeListener { envelope, fromUser ->
                        if (fromUser) {
                            colorList[i] = envelope.color
                            saveColor(i, colorList[i])
                            updateRemoteColorInfo()
                            binding.imgColor.setImageDrawable(ColorDrawable(envelope.color))
                        }
                    })
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.cancel()
                    }
                    .setTitle(getString(R.string.tv_title_color_picker, i + 1))
                    .attachAlphaSlideBar(false)
                    .also {
                        it.colorPickerView.post {
                            kotlin.runCatching {
                                it.colorPickerView.selectByHsvColor(colorList[i])
                            }
                        }
                    }
                    .show()

            }
            layout2.colorList.addView(binding.root)
        }
    }

    override fun onResume() {
        super.onResume()
        val pingBinder = Shizuku.pingBinder()
        binding.layout1.loop(pingBinder)
    }

    /** 避免onCreate之后 重复展示对话框 */
    private var showShizukuDialog = false

    /** 如果 版本过低, 则弹出 dialog */
    private fun checkShizukuVersion(): Boolean{
        if (!showShizukuDialog && Shizuku.isPreV11()) {
            showShizukuDialog = true
            // Pre-v11 is unsupported
            MaterialAlertDialogBuilder(this@MainActivity)
                .setMessage(getString(R.string.shizuku_version_low))
                .setCancelable(false)
                .setPositiveButton("Go"){dialog, _->
                    openGooglePlayStore(PKG_SHIZUKU)
                    finishAndRemoveTask()
                    exitProcess(0)
                }
                .setNegativeButton("Exit"){dialog, _->
                    finishAndRemoveTask()
                    exitProcess(0)
                }
                .show()
            return false
        }
        return true
    }

    private fun tipUserGoGrantPermission(){
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.open_shizuku_grant))
            .setPositiveButton("Go"){dialog, _->
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setPackage(PKG_SHIZUKU)
                startActivity(intent)
                dialog.cancel()
            }
            .setNegativeButton("Cancel"){dialog, _->
                dialog.cancel()
            }
            .show()
    }

    /**
     * 在Shizuku运行状态变化(pingBinder的返回值变化)、
     * 自身App授权可能发生变化(例如切换到Shizuku界面然后又切换回来调用了onResume)时调用。
     */
    private fun ActivityMainCard1Binding.loop(pingBinder: Boolean){
        if (pingBinder) {
            tvStep1.visibility = View.GONE
            btStep1.visibility = View.GONE
            tvStartTip.visibility = View.VISIBLE
            btStart.visibility = View.VISIBLE
            if(checkShizukuVersion() && mKernelInterface?.asBinder()?.isBinderAlive != true){
                if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED){
                    connectServer()
                }else{
                    binding.layout1.btStart.text = getString(R.string.bt_request_shizuku_permission)
                }
            }
        } else {
            tvStep1.visibility = View.VISIBLE
            btStep1.visibility = View.VISIBLE
            tvStartTip.visibility = View.GONE
            btStart.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showShizukuDialog = false

        binding.initPrefPanel()

        binding.initLink()

        binding.initColor()


        lifecycleScope.launchWhenStarted {
            var lastPingResult = false

            withContext(Dispatchers.IO){
                while(true){
                    val pingBinder = Shizuku.pingBinder()
                    if(lastPingResult != pingBinder){
                        Log.d(TAG, "onCreate: pingBinder = $pingBinder")
                        withContext(Dispatchers.Main){
                            binding.layout1.loop(pingBinder)
                        }
                        lastPingResult = pingBinder
                    }
                    if(pingBinder){
                        delay(5000)
                    }else{
                        delay(1000)
                    }
                }
            }
        }

        Shizuku.addRequestPermissionResultListener(this)

        binding.layout1.btStep1.setOnClickListener {
            openGooglePlayStore(PKG_SHIZUKU)
        }

        binding.layout1.btStart.setOnClickListener {
            if(!Shizuku.pingBinder()){
                Toast.makeText(this, getString(R.string.toast_check_shizuku_launch),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val kernelInterface = mKernelInterface
            if(kernelInterface?.asBinder()?.isBinderAlive == true){
                if(kernelInterface.isRunning){
                    kernelInterface.stop()
                    binding.layout1.btStart.text = getString(R.string.bt_start)

                }else{
                    val inputPath = try{
                        kernelInterface.inputPath
                    }catch (e: Exception){
                        Toast.makeText(this, "error: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "onCreate: kernelInterface.inputPath", e)
                        return@setOnClickListener
                    }
                    Log.i(TAG, "onServiceConnected: $inputPath")
                    if(inputPath != null){
                        Log.i(TAG, "onServiceConnected: ${Arrays.toString(inputPath)}")
                    }

                    if(inputPath == null || inputPath.isEmpty()){
                        Log.e(TAG, "onServiceConnected: inputPath == null.")
                        Toast.makeText(this@MainActivity,
                            getString(R.string.toast_no_input_device),
                            Toast.LENGTH_SHORT).show()
                        binding.layout1.btStart.text = getString(R.string.bt_device_not_support)
                        return@setOnClickListener
                    }

                    var path = inputPath[0].path
                    var weight = inputPath[0].weight
                    inputPath.forEach {
                        if(it.weight > weight){
                            weight = it.weight
                            path = it.path
                        }
                    }
                    if(! kernelInterface.start(path, colorList,
                            getTouchPointSize(), getTouchPathDisappearanceTime(),
                            getStrokeWidthCircle(), getStrokeWidthLine(), getAlpha())
                        ){
                        Toast.makeText(this@MainActivity,
                            getString(R.string.toast_launch_fail),
                            Toast.LENGTH_SHORT).show()
                        binding.layout1.btStart.isEnabled = true
                    }
                    binding.layout1.btStart.text = getString(R.string.bt_stop)

                }

                return@setOnClickListener
            }

            if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED){
                connectServer()
            }else if (Shizuku.shouldShowRequestPermissionRationale()) {
                // Users choose "Deny and don't ask again"
                tipUserGoGrantPermission()
            } else {
                Shizuku.requestPermission(REQ_CODE_SHIZUKU)
            }
        }

    }

    private val serviceArgs by lazy {
        val componentName = ComponentName(packageName, KernelService::class.java.name)
        Shizuku.UserServiceArgs(componentName).also {serviceArgs->
            serviceArgs.processNameSuffix("k3x1n")
            serviceArgs.debuggable(true)
            serviceArgs.version(BuildConfig.VERSION_CODE)
        }
    }

    private fun connectServer(){
        binding.layout1.btStart.isEnabled = false
        binding.layout1.btStart.text = getString(R.string.bt_start_service)
        Shizuku.bindUserService(serviceArgs, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service == null) {
                    mKernelInterface = null
                    binding.layout1.btStart.isEnabled = true
                    binding.layout1.btStart.text = getString(R.string.bt_retry_connect_server)
                    Log.e(TAG, "onServiceConnected: service == null.")
                    return
                }

                val kernelInterface = KernelInterface.Stub.asInterface(service)
                mKernelInterface = kernelInterface

                binding.layout1.btStart.isEnabled = true
                if (kernelInterface.isRunning) {
                    binding.layout1.btStart.text = getString(R.string.bt_stop)
                }else{
                    binding.layout1.btStart.text = getString(R.string.bt_start)
                }

                Log.d(TAG, "onServiceConnected: ")

            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e(TAG, "onServiceDisconnected: $name")
                mKernelInterface = null
                binding.layout1.btStart.isEnabled = true
                binding.layout1.btStart.text = getString(R.string.bt_start_fail_retry)

            }

        })

    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "onRequestPermissionResult: $granted")
        if(granted){
            connectServer()
        }
    }

}