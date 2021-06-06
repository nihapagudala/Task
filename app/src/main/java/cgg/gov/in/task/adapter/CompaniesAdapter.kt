package cgg.gov.`in`.task.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cgg.gov.`in`.task.R
import cgg.gov.`in`.task.databinding.ItemAttendanceBinding
import cgg.gov.`in`.task.model.CompaniesRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class CompaniesAdapter(private val context: Context, list: List<CompaniesRes>?) :
    RecyclerView.Adapter<CompaniesAdapter.ItemHolder>() {
    private val list: List<CompaniesRes>?

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val listItemBinding: ItemAttendanceBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_attendance, parent, false
        )
        return ItemHolder(listItemBinding)
    }

    override fun onBindViewHolder(holder: ItemHolder, i: Int) {
        val dataModel: CompaniesRes = list!![i]

        val distance = (dataModel.distance) / 1000
        holder.listItemBinding.tvName.setText(dataModel.company_name)
        holder.listItemBinding.tvDes.setText(dataModel.company_description)
        holder.listItemBinding.tvRating.setText(dataModel.avg_rating.toString())
        holder.listItemBinding.tvDistance.setText("%.1f".format(distance) + " km")
        holder.listItemBinding.ratingbar.rating = dataModel.avg_rating.toFloat()

        if (!TextUtils.isEmpty(dataModel.company_image_url)) {
            holder.listItemBinding.pBar.setVisibility(View.VISIBLE)
            Glide.with(context)
                .load(dataModel.company_image_url)
                .error(R.drawable.login_user)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.listItemBinding.pBar.setVisibility(View.GONE)
                        holder.listItemBinding.ivUser.setVisibility(View.VISIBLE)
                        holder.listItemBinding.ivUser.setImageDrawable(
                            context.resources.getDrawable(
                                R.drawable.login_user
                            )
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.listItemBinding.pBar.setVisibility(View.GONE)
                        holder.listItemBinding.ivUser.setVisibility(View.VISIBLE)
                        return false
                    }
                })
                .into(holder.listItemBinding.ivUser)

        }
    }

    override fun getItemCount(): Int {
        return if (list != null && list.size > 0) list.size else 0
    }

    class ItemHolder(listItemBinding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(listItemBinding.getRoot()) {
        var listItemBinding: ItemAttendanceBinding
        fun bind(obj: Any?) {
            listItemBinding.executePendingBindings()
        }

        init {
            this.listItemBinding = listItemBinding
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    init {
        this.list = list
    }
}
