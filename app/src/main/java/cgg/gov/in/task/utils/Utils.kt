package cgg.gov.`in`.task.utils

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
    }
}