package show.taps

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// todo
class TileService : TileService() {
    companion object{
        private const val TAG = "TileService"
    }

    private var myScope: CoroutineScope? = null

    override fun onClick() {
        super.onClick()
        val kernel = App.iKernel.value ?: return
        try{
            if(kernel.isRunning){
                kernel.stop()
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()

            }else{
                //todo:
                // kernel.start()
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.updateTile()

            }

        }catch (e: Exception){
            Log.e(TAG, "onClick: ", e)
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
            if(kernel == null){
                if(!App.bindServiceState.replayCache.contains(BindServiceState.Connecting)){
                    App.connectServer()
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