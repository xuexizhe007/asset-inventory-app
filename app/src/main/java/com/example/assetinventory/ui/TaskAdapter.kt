package com.example.assetinventory.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assetinventory.R
import com.example.assetinventory.data.TaskInfo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private var items: List<TaskInfo>,
    private val onItemClick: (TaskInfo) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    fun update(newItems: List<TaskInfo>) {
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
        private val tvTaskInfo: TextView = itemView.findViewById(R.id.tvTaskInfo)

        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(task: TaskInfo) {
            tvTaskName.text = "${task.name} (${task.category})" // Display asset category with asset name
            val timeText = sdf.format(Date(task.createdAt))
            tvTaskInfo.text = "导入时间：$timeText    共 ${task.assetCount} 条资产"
            itemView.setOnClickListener { onItemClick(task) }
        }
    }
}
