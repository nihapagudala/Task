package cgg.gov.`in`.task.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import cgg.gov.`in`.task.R
import cgg.gov.`in`.task.databinding.CustominfowindowBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker


class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

    private var context: Activity? = null

    constructor(context: Activity?) {
        this.context = context
    }

    override fun getInfoWindow(p0: Marker): View? {
        return null
    }

    override fun getInfoContents(marker: Marker): View? {
        val binding: CustominfowindowBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.custominfowindow,
            null,
            false
        )

        if (marker.title.equals(context!!.resources.getString(R.string.current_loc))) {
            binding.ivStar.visibility = View.GONE
            binding.tvDes.visibility = View.VISIBLE
            binding.tvDes.setText(marker.getSnippet())
        }else{
            binding.ivStar.visibility = View.VISIBLE
            binding.tvDes.visibility = View.GONE
        }
        binding.tvTitle.setText(marker.getTitle())
        return binding.root
    }
}