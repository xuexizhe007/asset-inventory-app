package com.example.assetinventory.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

class AssetAdapter(
    private var items: List<Asset>,
    private val onItemClick: (Asset) -> Unit
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    fun update(newItems: List<Asset>) {
        items = newItems
        notifyDataSetChanged()
    }

    val currentItems: List<Asset>
        get() = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
        private val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvUserDept: TextView = itemView.findViewById(R.id.tvUserDept)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvStartDate: TextView = itemView.findViewById(R.id.tvStartDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(asset: Asset) {
            tvCode.text = asset.code
            
            val categoryText = asset.category.orEmpty()
            // 简单显示，UI已做加粗处理
            tvName.text = "${asset.name} " + if(categoryText.isNotEmpty()) "($categoryText)" else ""
            
            tvUserDept.text = "${asset.user.orEmpty()} · ${asset.department.orEmpty()}"
            tvLocation.text = asset.location.orEmpty()
            tvStartDate.text = asset.startDate
            tvStatus.text = asset.status.displayName

            // 适配新的 UI 风格：修改文字颜色和背景色，而不是 GradientDrawable
            val context = itemView.context
            val (bgColorRes, textColorRes) = when (asset.status) {
                AssetStatus.UNCHECKED -> R.color.status_unchecked_bg to R.color.status_unchecked
                AssetStatus.MATCHED -> R.color.status_matched_bg to R.color.status_matched
                AssetStatus.MISMATCH -> R.color.status_mismatch_bg to R.color.status_mismatch
                AssetStatus.LABEL_REPRINT -> R.color.status_reprint_bg to R.color.status_reprint
            }
            
            tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, bgColorRes))
            tvStatus.setTextColor(ContextCompat.getColor(context, textColorRes))

            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = asset.selectedForPrint
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                asset.selectedForPrint = isChecked
            }

            itemView.setOnClickListener {
                onItemClick(asset)
            }
        }
    }
}
