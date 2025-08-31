package com.example.smartplanner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartplanner.adapter.TaskAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    //pomodoro
    // Declare these at the top of your Activity class if not already
    private lateinit var pomodoroTextView: TextView
    private lateinit var pomodoroButton: Button
    private lateinit var pomodoroResetButton: Button

    private var pomodoroCountDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var isPaused = false
    private var timeLeftInMillis: Long = 25 * 60 * 1000 // 25 minutes


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🔔 Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_home)

        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        auth = FirebaseAuth.getInstance()

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }

                else -> false
            }
        }


        setupRecyclerView()
        fetchTasksFromFirestore()

        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // Initialize UI
        pomodoroTextView = findViewById(R.id.tvPomodoroTimer)
        pomodoroButton = findViewById(R.id.btnStartPomodoro)
        pomodoroResetButton = findViewById(R.id.btnResetPomodoro)

        updateTimerText()

        pomodoroButton.setOnClickListener {
            if (!isTimerRunning && !isPaused) {
                // Start fresh
                startPomodoroTimer()
                pomodoroButton.text = "Pause"
                pomodoroResetButton.visibility = Button.VISIBLE
            } else if (isTimerRunning) {
                // Pause
                pausePomodoroTimer()
                pomodoroButton.text = "Resume"
            } else if (isPaused) {
                // Resume
                resumePomodoroTimer()
                pomodoroButton.text = "Pause"
            }
        }

        pomodoroResetButton.setOnClickListener {
            resetPomodoroTimer()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(taskList) { deletedTaskId ->
            // Optional: you can log or refresh here
        }
        recyclerView.adapter = taskAdapter
    }

    private fun fetchTasksFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid

        userId?.let {
            db.collection("tasks")
                .document(userId)
                .collection("userTasks")
                .get()
                .addOnSuccessListener { result ->
                    taskList.clear()
                    for (document in result) {
                        val task = document.toObject(Task::class.java).copy(id = document.id)
                        taskList.add(task)
                    }
                    taskAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("HomeActivity", "Error fetching tasks: ", e)
                }
        }
    }

    private fun addTaskToFirestore(
        title: String,
        description: String,
        dueDate: String,
        priority: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid

        val newTask = Task(
            title = title,
            description = description,
            timestamp = System.currentTimeMillis(),
            dueDate = dueDate,
            priority = priority,
            isCompleted = false
        )

        userId?.let {
            db.collection("tasks")
                .document(userId)
                .collection("userTasks")
                .add(newTask)
                .addOnSuccessListener {
                    Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show()
                    fetchTasksFromFirestore()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_task_dialog, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.editTaskTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTaskDescription)
        val dueDateInput = dialogView.findViewById<EditText>(R.id.editDueDate)
        val spinnerPriority = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerPriority)

        // Priority Options
        val priorities = listOf("Low", "Medium", "High")
        val adapter =
            android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        // Date Picker
        dueDateInput.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)

            val datePickerDialog = android.app.DatePickerDialog(this, { _, y, m, d ->
                // After date is selected, show time picker
                val timePickerDialog =
                    android.app.TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                        val formattedDateTime = String.format(
                            "%02d/%02d/%04d %02d:%02d",
                            d, m + 1, y, selectedHour, selectedMinute
                        )
                        dueDateInput.setText(formattedDateTime)
                    }, hour, minute, true)

                timePickerDialog.show()
            }, year, month, day)

            datePickerDialog.show()
        }

        // Build Alert Dialog
        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val dueDate = dueDateInput.text.toString().trim()
                val priority = spinnerPriority.selectedItem.toString()

                if (title.isNotEmpty()) {
                    // Parse the due date and time into a Calendar object
                    val dateParts = dueDate.split(" ")
                    val date = dateParts[0] // This will be "dd/MM/yyyy"
                    val time = dateParts[1] // This will be "HH:mm"

                    val datePartsArray = date.split("/")
                    val timePartsArray = time.split(":")

                    // Get day, month, year, hour, and minute
                    val day = datePartsArray[0].toInt()
                    val month = datePartsArray[1].toInt() - 1 // Month is 0-indexed
                    val year = datePartsArray[2].toInt()
                    val hour = timePartsArray[0].toInt()
                    val minute = timePartsArray[1].toInt()

                    // Create a Calendar object
                    val calendar = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.YEAR, year)
                        set(java.util.Calendar.MONTH, month)
                        set(java.util.Calendar.DAY_OF_MONTH, day)
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, minute)
                        set(
                            java.util.Calendar.SECOND,
                            0
                        ) // Set seconds to 0 to trigger alarm at exact time
                    }

                    // Now you can use this calendar object to schedule the alarm
                    scheduleAlarm(calendar)

                    addTaskToFirestore(title, description, dueDate, priority)
                } else {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Method to schedule the alarm
// Method to schedule the alarm
    private fun scheduleAlarm(calendar: Calendar) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Get the time in milliseconds from the Calendar object
        val timeInMillis = calendar.timeInMillis

        // Check if we can schedule exact alarms (required for Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Request permission to schedule exact alarms if needed
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return  // Exit early, don't schedule alarm yet
            }
        }

        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("taskId", "some_unique_task_id")
        // Use FLAG_UPDATE_CURRENT as fallback for backward compatibility with older API levels
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Schedule the alarm using AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }


    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                auth.signOut()
                startActivity(Intent(this@HomeActivity, MainActivity::class.java))
                finish()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun startPomodoroTimer() {
        pomodoroCountDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                isTimerRunning = false
                pomodoroButton.text = "Start"
                pomodoroResetButton.visibility = Button.GONE
            }
        }.start()

        isTimerRunning = true
        isPaused = false
    }

    private fun pausePomodoroTimer() {
        pomodoroCountDownTimer?.cancel()
        isTimerRunning = false
        isPaused = true
    }

    private fun resumePomodoroTimer() {
        startPomodoroTimer()
    }

    private fun resetPomodoroTimer() {
        pomodoroCountDownTimer?.cancel()
        timeLeftInMillis = 25 * 60 * 1000
        updateTimerText()
        isTimerRunning = false
        isPaused = false
        pomodoroButton.text = "Start"
        pomodoroResetButton.visibility = Button.GONE
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        pomodoroTextView.text = String.format("%02d:%02d", minutes, seconds)
    }
}