package cgg.gov.`in`.task.error_handler

import android.content.Context
import cgg.gov.`in`.task.R
import cgg.gov.`in`.task.utils.Utils
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class ErrorHandler {
    companion object {
        fun handleError(e: Throwable?, context: Context?): String {
            var msg = ""
            if (e is HttpException) {
                val responseBody = e.response()!!.errorBody()
                msg = responseBody?.let { Utils.getErrorMessage(it) }!!
            } else if (e is SocketTimeoutException) {
                msg = context?.getString(R.string.con_time_out).toString()
            } else if (e is MalformedJsonException) {
                msg = context?.getString(R.string.something) + "\n" +
                        context?.getString(R.string.jsonreader)
            } else if (e is IOException) {
                msg = context?.getString(R.string.something) + " :IO Exception"
            } else {
                msg = context?.getString(R.string.server_not) + ": " + (e?.message)
            }
            return msg
        }
    }
}