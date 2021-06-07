package cgg.gov.`in`.task.utils

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import okhttp3.ResponseBody
import org.json.JSONObject

class Utils {
    companion object {
        fun getErrorMessage(responseBody: ResponseBody): String? {
            return try {
                val jsonObject = JSONObject(responseBody.string())
                jsonObject.getString("message")
            } catch (e: Exception) {
                e.message
            }
        }

        fun calcDistance(crtLocation: Location, desLocation: Location?): Float {
            return crtLocation.distanceTo(desLocation) // in meters
        }
        fun checkInternetConnection(context: Context): Boolean {
            val conMgr = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return (conMgr?.activeNetworkInfo != null && conMgr.activeNetworkInfo!!.isAvailable
                    && conMgr.activeNetworkInfo!!.isConnected)
        }
    }
}