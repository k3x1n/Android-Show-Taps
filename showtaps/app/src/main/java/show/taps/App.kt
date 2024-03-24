package show.taps

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import rikka.shizuku.Shizuku
import show.taps.server.GlobalSettings
import show.taps.server.KernelService

class App: Application() {

    companion object{

        private const val TAG = "App"

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: App

        val iKernel = MutableStateFlow<KernelInterface?>(null)

        val bindServiceState = MutableSharedFlow<BindServiceState>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val stateChangeFlow = MutableSharedFlow<Int>()

        /** 长度必须是 [GlobalSettings.COLOR_ARRAY_LENGTH] */
        lateinit var colorList : IntArray

        private val serviceArgs by lazy {
            val componentName = ComponentName(instance.packageName, KernelService::class.java.name)
            Shizuku.UserServiceArgs(componentName).also { serviceArgs->
                serviceArgs.processNameSuffix("k3x1n")
                serviceArgs.debuggable(true)
                serviceArgs.version(BuildConfig.VERSION_CODE)
            }
        }

        fun connectServer(){
            bindServiceState.tryEmit(BindServiceState.Connecting)
            Shizuku.bindUserService(serviceArgs, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Log.d(TAG, "onServiceConnected() called with: name = $name, service = $service")
                    if (service == null) {
                        onServiceDisconnected(name)
                        Log.e(TAG, "onServiceConnected: service == null.")
                        return
                    }
                    iKernel.value = KernelInterface.Stub.asInterface(service)
                    bindServiceState.tryEmit(BindServiceState.Success)
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.e(TAG, "onServiceDisconnected: $name")
                    iKernel.value = null
                    bindServiceState.tryEmit(BindServiceState.Fail)
                }
            })
        }



    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        instance = this
        colorList = getColorList()

    }



}