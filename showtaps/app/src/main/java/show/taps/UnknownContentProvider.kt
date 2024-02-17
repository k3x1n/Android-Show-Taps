package show.taps

import android.content.AttributionSource
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.IContentProvider
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ICancellationSignal
import android.os.RemoteCallback
import java.util.ArrayList

class UnknownContentProvider : IContentProvider {
    override fun asBinder(): IBinder? = null

    override fun query(
        attributionSource: AttributionSource?,
        url: Uri?,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: ICancellationSignal?
    ) = null

    override fun getType(attributionSource: AttributionSource?, url: Uri?) = null

    override fun getTypeAsync(
        attributionSource: AttributionSource?,
        url: Uri?,
        callback: RemoteCallback?
    ) {
    }

    override fun getTypeAnonymousAsync(uri: Uri?, callback: RemoteCallback?) {
    }

    override fun insert(
        attributionSource: AttributionSource?,
        url: Uri?,
        initialValues: ContentValues?,
        extras: Bundle?
    ) = null

    override fun bulkInsert(
        attributionSource: AttributionSource?,
        url: Uri?,
        initialValues: Array<out ContentValues>?
    ) = 0

    override fun delete(attributionSource: AttributionSource?, url: Uri?, extras: Bundle?) = 0

    override fun update(
        attributionSource: AttributionSource?,
        url: Uri?,
        values: ContentValues?,
        extras: Bundle?
    ) = 0

    override fun openFile(
        attributionSource: AttributionSource?,
        url: Uri?,
        mode: String?,
        signal: ICancellationSignal?
    ) = null

    override fun openAssetFile(
        attributionSource: AttributionSource?,
        url: Uri?,
        mode: String?,
        signal: ICancellationSignal?
    ) = null

    override fun applyBatch(
        attributionSource: AttributionSource?,
        authority: String?,
        operations: ArrayList<ContentProviderOperation>?
    ) = null

    override fun call(
        attributionSource: AttributionSource?,
        authority: String?,
        method: String?,
        arg: String?,
        extras: Bundle?
    ) = null

    override fun checkUriPermission(
        attributionSource: AttributionSource?,
        uri: Uri?,
        uid: Int,
        modeFlags: Int
    ) = PackageManager.PERMISSION_GRANTED

    override fun createCancellationSignal() = null

    override fun canonicalize(attributionSource: AttributionSource?, uri: Uri?) = uri

    override fun canonicalizeAsync(
        attributionSource: AttributionSource?,
        uri: Uri?,
        callback: RemoteCallback?
    ) {
    }

    override fun uncanonicalize(attributionSource: AttributionSource?, uri: Uri?) = uri

    override fun uncanonicalizeAsync(
        attributionSource: AttributionSource?,
        uri: Uri?,
        callback: RemoteCallback?
    ) {
    }

    override fun refresh(
        attributionSource: AttributionSource?,
        url: Uri?,
        extras: Bundle?,
        cancellationSignal: ICancellationSignal?
    ) = true

    override fun getStreamTypes(
        attributionSource: AttributionSource?,
        url: Uri?,
        mimeTypeFilter: String?
    ) = null

    override fun openTypedAssetFile(
        attributionSource: AttributionSource?,
        url: Uri?,
        mimeType: String?,
        opts: Bundle?,
        signal: ICancellationSignal?
    ) = null

}
