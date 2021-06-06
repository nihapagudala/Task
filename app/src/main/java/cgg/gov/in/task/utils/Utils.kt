package cgg.gov.`in`.task.utils

import android.location.Location
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

    }
}