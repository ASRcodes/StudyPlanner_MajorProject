package com.example.smartplanner.adapter

import android.app.AlertDialog // ✅ IMPORT THIS
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartplanner.R
import com.example.smartplanner.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val onTaskDeleted: (String) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.taskName)
        val description: TextView = itemView.findViewById(R.id.taskDescription)
        val dueDate: TextView = itemView.findViewById(R.id.taskDueDate)
        val priority: TextView = itemView.findViewById(R.id.taskPriority)
        val checkBox: CheckBox = itemView.findViewById(R.id.taskCheckBox)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteTaskButton)
        val editButton: ImageButton = itemView.findViewById(R.id.editTaskButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_layout, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.name.text = task.title
        holder.description.text = task.description
        holder.dueDate.text = if (task.dueDate.isNotEmpty()) "Due: ${task.dueDate}" else "No due date"
        holder.priority.text = task.priority?.replaceFirstChar { it.uppercase() } ?: "None"
        holder.checkBox.isChecked = task.isCompleted

        val priorityColor = when (task.priority?.lowercase()) {
            "high" -> Color.RED
            "medium" -> Color.parseColor("#FFA500")
            "low" -> Color.parseColor("#4CAF50")
            else -> Color.GRAY
        }
        holder.priority.setBackgroundColor(priorityColor)

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = task.isCompleted

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val context = holder.itemView.context
            val taskId = task.id ?: return@setOnCheckedChangeListener

            if (task.isCompleted) {
                // Ignore toggle if already completed
                holder.checkBox.isChecked = true
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                // Ask for confirmation if the task is completed
                AlertDialog.Builder(context)
                    .setTitle("Confirm Completion")
                    .setMessage("Are you sure this task is completed?")
                    .setPositiveButton("Yes") { _, _ ->
                        // If confirmed, show the motivational dialog
                        AlertDialog.Builder(context)
                            .setTitle("🎉 Yay!")
                            .setMessage("You completed this task. Great job! 💪")
                            .setPositiveButton("Thanks!") { _, _ ->
                                updateTaskCompletionStatus(taskId, true)
                                task.isCompleted = true
                                taskList.removeAt(position)
                                taskList.add(0, task)
                                notifyItemMoved(position, 0)
                                notifyItemChanged(0)
                            }
                            .setCancelable(false)
                            .show()
                    }
                    .setNegativeButton("No") { _, _ ->
                        // Uncheck the box if user cancels
                        holder.checkBox.isChecked = false
                    }
                    .setCancelable(false)
                    .show()
            } else {
                updateTaskCompletionStatus(taskId, false)
                task.isCompleted = false
                taskList.removeAt(position)
                taskList.add(task)
                notifyDataSetChanged()
            }
        }
        holder.editButton.isEnabled = !task.isCompleted
        // Prevent clicks on completed tasks to avoid crash
        holder.itemView.setOnClickListener {
            if (!task.isCompleted) {
                // Optional: do something on click (like open details or edit)
            }
        }

        holder.editButton.setOnClickListener {
            val context = holder.itemView.context
            val taskId = task.id ?: return@setOnClickListener

            val editView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_task, null)

            val titleInput = editView.findViewById<TextView>(R.id.editTaskTitle)
            val descInput = editView.findViewById<TextView>(R.id.editTaskDescription)

            // Set current values
            titleInput.text = task.title
            descInput.text = task.description

            AlertDialog.Builder(context)
                .setTitle("Edit Task")
                .setView(editView)
                .setPositiveButton("Save") { _, _ ->
                    val newTitle = titleInput.text.toString()
                    val newDesc = descInput.text.toString()

                    updateTaskInFirestore(taskId, newTitle, newDesc, position)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        holder.deleteButton.setOnClickListener {
            val context = holder.itemView.context
            val taskId = task.id ?: return@setOnClickListener

            AlertDialog.Builder(context)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteTaskFromFirestore(taskId, position)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }



    private fun updateTaskInFirestore(taskId: String, newTitle: String, newDesc: String, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val updates = mapOf(
            "title" to newTitle,
            "description" to newDesc
        )

        db.collection("tasks")
            .document(userId)
            .collection("userTasks")
            .document(taskId)
            .update(updates)
            .addOnSuccessListener {
                taskList[position].title = newTitle
                taskList[position].description = newDesc
                notifyItemChanged(position)
            }
            .addOnFailureListener {
                // Optional: Toast for error
            }
    }


    // ✅ Move these methods OUTSIDE of onBindViewHolder
    private fun deleteTaskFromFirestore(taskId: String, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("tasks")
            .document(userId)
            .collection("userTasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener {
                taskList.removeAt(position)
                notifyItemRemoved(position)
                onTaskDeleted(taskId)
            }
            .addOnFailureListener { e ->
                // Optional: show error
            }
    }

    private fun updateTaskCompletionStatus(taskId: String, isCompleted: Boolean) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("tasks")
            .document(userId)
            .collection("userTasks")
            .document(taskId)
            .update("completed", isCompleted)
    }

    override fun getItemCount(): Int = taskList.size
}
