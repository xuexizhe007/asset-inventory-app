package com.example.assetinventory.ui

import android.graphics.drawable.GradientDrawable
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
            tvCode.text = "资产编码：${asset.code}"
            
            // 修改：同时显示名称和类别
            val categoryText = asset.category.orEmpty()
            tvName.text = "资产名称：${asset.name}\n资产类别：$categoryText"
            
            tvUserDept.text = "使用人：${asset.user.orEmpty()} / 使用部门：${asset.department.orEmpty()}"
            tvLocation.text = "存放地点：${asset.location.orEmpty()}"
            tvStartDate.text = "投用日期：${asset.startDate}"
            tvStatus.text = asset.status.displayName

            val context = itemView.context
            val colorRes = when (asset.status) {
                AssetStatus.UNCHECKED -> R.color.status_unchecked
                AssetStatus.MATCHED -> R.color.status_matched
                AssetStatus.MISMATCH -> R.color.status_mismatch
                AssetStatus.LABEL_REPRINT -> R.color.status_reprint
            }
            val bgColor = ContextCompat.getColor(context, colorRes)
            val drawable = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(bgColor)
            }
            tvStatus.background = drawable

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
