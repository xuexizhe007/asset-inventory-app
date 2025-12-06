package com.example.assetinventory.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.model.Asset
import com.example.assetinventory.model.AssetStatus

class AssetAdapter(
    private var items: List<Asset>,
    private val onItemClick: (Asset) -> Unit
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    val currentItems: List<Asset>
        get() = items

    fun update(newItems: List<Asset>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset, parent, false)
        return AssetViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val asset = items[position]
        holder.bind(asset)
    }

    override fun getItemCount(): Int = items.size

    class AssetViewHolder(
        itemView: View,
        private val onItemClick: (Asset) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
        private val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvUserDept: TextView = itemView.findViewById(R.id.tvUserDept)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvStartDate: TextView = itemView.findViewById(R.id.tvStartDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val viewStatusBar: View = itemView.findViewById(R.id.viewStatusBar)

        fun bind(asset: Asset) {
            // 基本信息
            tvCode.text = "资产编码：${asset.code}"
            tvName.text = "资产名称：${asset.name}"

            val user = asset.user.orEmpty()
            val dept = asset.department.orEmpty()
            tvUserDept.text = "使用人：$user / 部门：$dept"

            tvLocation.text = "存放地点：${asset.location.orEmpty()}"
            tvStartDate.text = "投用日期：${asset.startDate.orEmpty()}"

            // 选择复选框（用于打印标签）
            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = asset.selectedForPrint
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                asset.selectedForPrint = isChecked
            }

            // 状态标签 + 左侧色条
            when (asset.status) {
                AssetStatus.UNCHECKED -> {
                    tvStatus.text = "● 未盘点"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_unchecked)
                    viewStatusBar.setBackgroundResource(R.color.status_unchecked)
                }
                AssetStatus.MATCHED -> {
                    tvStatus.text = "● 相符"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_matched)
                    viewStatusBar.setBackgroundResource(R.color.status_matched)
                }
                AssetStatus.MISMATCH -> {
                    tvStatus.text = "● 不相符"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_mismatch)
                    viewStatusBar.setBackgroundResource(R.color.status_mismatch)
                }
                AssetStatus.LABEL_REPRINT -> {
                    tvStatus.text = "● 补打标签"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_reprint)
                    viewStatusBar.setBackgroundResource(R.color.status_reprint)
                }
            }

            itemView.setOnClickListener {
                onItemClick(asset)
            }
        }
    }
}
