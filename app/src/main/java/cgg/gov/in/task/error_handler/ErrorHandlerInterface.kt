package cgg.gov.`in`.task.error_handler

import android.content.Context

interface ErrorHandlerInterface {
    fun handleError(e: Throwable?, context: Context?)
    fun handleError(e: String?, context: Context?)
}