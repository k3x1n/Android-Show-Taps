package show.taps

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.util.Log

private const val TAG = "Goto"

fun Activity.openGooglePlayStore(pkg: String = applicationContext.packageName){
    try{
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage("com.android.vending")
        intent.data = Uri.parse("market://details?id=$pkg")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        applicationContext.startActivity(intent)
    }catch (e: ActivityNotFoundException){
        Log.e(TAG, "openGooglePlayStore: ", e)
        openWebBrowser("https://play.google.com/store/apps/details?id=$pkg")
    }
}


fun Activity.openWebBrowser(url: String, pkg: String? = null) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    if(pkg != null){
        intent.setPackage(pkg)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try{
        val resolveActivity = intent.resolveActivity(applicationContext.packageManager)
        Log.d(TAG, "openWebBrowser: resolveActivity = $resolveActivity")
        if (resolveActivity != null) {
            applicationContext.startActivity(Intent.createChooser(intent, "Open").also{
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)})
        }else{
            applicationContext.startActivity(intent)
        }
    }catch (e: Exception){
        Log.e(TAG, "openWebBrowser: ", e)
    }
}

fun Activity.copy(content: String): Boolean{
    try{
        val clipboardManager = getSystemService(ClipboardManager::class.java)
        clipboardManager.primaryClip?.addItem(ClipData.Item(content))
        val label = getString(R.string.app_name)
        clipboardManager.primaryClip = ClipData.newPlainText(label, content)
        return true
    }catch (e: Exception){
        Log.e(TAG, "copy: ", e)
    }
    return false
}
