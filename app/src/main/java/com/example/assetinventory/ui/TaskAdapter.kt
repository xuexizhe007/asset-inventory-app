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
    private val onClick: (InventoryTask) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun update(newItems: List<InventoryTask>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(v)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvTaskName)
        private val tvInfo: TextView = view.findViewById(R.id.tvTaskInfo)
        private val tvCreated: TextView = view.findViewById(R.id.tvTaskCreatedAt)

        fun bind(task: InventoryTask) {
            tvName.text = task.name
            tvInfo.text = "共 ${task.assets.size} 条资产"
            tvCreated.text = "创建时间：" + sdf.format(Date(task.createdAt))
            itemView.setOnClickListener { onClick(task) }
        }
    }
}
