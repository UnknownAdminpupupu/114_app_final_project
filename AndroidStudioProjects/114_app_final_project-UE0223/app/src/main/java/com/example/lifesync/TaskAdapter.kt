package com.example.lifesync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lifesync.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Int) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.binding.tvTaskTitle.text = task.title

        // ===== 新增：顯示 DDL =====
        // 0L 代表略過 / 未設定
        holder.binding.tvTaskDeadline.text = if (task.deadlineAt <= 0L) {
            "DDL：未設定"
        } else {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
            "DDL：${sdf.format(Date(task.deadlineAt))}"
        }

        holder.itemView.setOnClickListener {
            onTaskClick(position)
        }
    }

    override fun getItemCount(): Int = tasks.size
}
