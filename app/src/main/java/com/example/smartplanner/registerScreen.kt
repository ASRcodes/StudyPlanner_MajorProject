package com.example.smartplanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartplanner.databinding.ActivityRegisterScreenBinding
import com.google.firebase.auth.FirebaseAuth

class registerScreen : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterScreenBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegisterScreenBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Register button clicked
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // Validate input
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user in Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        // Navigate to login screen or main screen
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Handle "Already have an account? Login here"
        binding.tvLoginHere.setOnClickListener {
            startActivity(Intent(this, LoginScreen::class.java))
        }

    }
}