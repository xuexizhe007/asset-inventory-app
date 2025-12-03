package com.example.assetinventory.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.model.InventoryTask
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private var items: List<InventoryTask>,
    private val onItemClick: (InventoryTask) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun update(newItems: List<InventoryTask>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val tvAssetCount: TextView = itemView.findViewById(R.id.tvAssetCount)

        fun bind(task: InventoryTask) {
            tvTaskName.text = task.name
            val context = itemView.context
            tvCreatedAt.text = context.getString(R.string.task_created_at, sdf.format(Date(task.createdAt)))
            tvAssetCount.text = context.getString(R.string.task_asset_count, task.assets.size)

            itemView.setOnClickListener {
                onItemClick(task)
            }
        }
    }
}
