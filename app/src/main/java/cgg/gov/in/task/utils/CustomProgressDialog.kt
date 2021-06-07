package cgg.gov.`in`.task.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import cgg.gov.`in`.task.R
import com.bumptech.glide.Glide

class CustomProgressDialog(context: Context?) : Dialog(
    context!!
) {
    private val mDialog: CustomProgressDialog? = null

    init {
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val view: View =
                LayoutInflater.from(context).inflate(R.layout.custom_progress_layout, null)
            val imageprogress = view.findViewById<ImageView>(R.id.imageprogress)
            Glide.with(context!!).load(R.drawable.loader_black1).into(imageprogress)
            //  customProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            setContentView(view)
            if (window != null) this.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
