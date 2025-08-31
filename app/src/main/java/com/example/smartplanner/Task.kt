package com.example.smartplanner

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var timestamp: Long = 0,
    var dueDate: String = "", // This should be stored in "yyyy-MM-dd" format
    var priority: String = "",
    var isCompleted: Boolean = false
)

