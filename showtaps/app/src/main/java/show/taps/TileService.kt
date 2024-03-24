package show.taps

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class TileService : TileService() {
    companion object{
        private const val TAG = "TileService"

        private val handler by lazy { Handler(Looper.getMainLooper()) }
    }

    private var myScope: CoroutineScope? = null

    private fun innerOnClick(kernel: KernelInterface){
        try{
            if(kernel.isRunning){
                kernel.stop()
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()

            }else{
                if(kernel.start(getLastDevName(), App.colorList,
                        getTouchPointSize(), getTouchPathDisappearanceTime(),
                        getStrokeWidthCircle(), getStrokeWidthLine(), getAlpha())
                    ){
                    qsTile.state = Tile.STATE_ACTIVE
                    qsTile.updateTile()
                }

            }

        }catch (e: Exception){
            Log.e(TAG, "onClick: ", e)
        }

    }

    private val runnable = Runnable{
        val kernel2 = App.iKernel.value
        if(kernel2 != null){
            innerOnClick(kernel2)
        }
    }

    override fun onClick() {
        super.onClick()
        val kernel = App.iKernel.value
        if(kernel == null || !kernel.asBinder().isBinderAlive){
            // waiting "App.connectServer()":
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 500)
        }else{
            innerOnClick(kernel)
        }
    }

    override fun onStartListening() {
        super.onStartListening()

        myScope = CoroutineScope(Dispatchers.Main).also {
            it.launch {
                App.iKernel.collectLatest { iKernel->
                    if(iKernel == null){
                        qsTile.state = Tile.STATE_INACTIVE
                        qsTile.updateTile()
                        return@collectLatest
                    }
                    qsTile.state = if(iKernel.isRunning){
                        Tile.STATE_ACTIVE
                    }else{
                        Tile.STATE_INACTIVE
                    }
                    qsTile.updateTile()
                }
            }
            it.launch {
                App.stateChangeFlow.collectLatest {
                    qsTile.state = if(App.iKernel.value?.isRunning == true){
                        Tile.STATE_ACTIVE
                    }else{
                        Tile.STATE_INACTIVE
                    }
                }
            }
            val kernel = App.iKernel.value
            if(kernel == null || !kernel.asBinder().isBinderAlive){
                try{
                    if(!App.bindServiceState.replayCache.contains(BindServiceState.Connecting)
                        && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED){
                        App.connectServer()
                    }
                }catch (e: Exception){
                    Log.e(TAG, "onStartListening: ", e)
                }
            }
        }

    }

    override fun onStopListening() {
        super.onStopListening()
        myScope?.cancel()
        myScope = null
    }

}