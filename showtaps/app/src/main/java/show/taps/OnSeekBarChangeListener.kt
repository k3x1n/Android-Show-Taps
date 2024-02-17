package show.taps

import android.widget.SeekBar

class OnSeekBarChangeListener(
    private val onProgressChanged: (progress: Int, fromUser: Boolean) -> Unit
): SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        onProgressChanged.invoke(progress, fromUser)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

}