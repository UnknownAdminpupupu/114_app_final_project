package com.example.lifesync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lifesync.databinding.ItemTaskBinding // 如果這裡報錯，請看下方說明建立 item_task.xml

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Int) -> Unit // 點擊事件回傳位置
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // 載入 item_task.xml 介面
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.tvTaskTitle.text = task.title

        // 點擊整列時觸發完成
        holder.itemView.setOnClickListener {
            onTaskClick(position)
        }
    }

    override fun getItemCount(): Int = tasks.size
}