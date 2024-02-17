package show.taps

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class DevInfo(
    val path: String,
    val name: String?,
    val width: Int,
    val height: Int,
    val type: Int,
    val weight: Int,
) : Parcelable