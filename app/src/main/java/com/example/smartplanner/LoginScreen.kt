package com.example.smartplanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartplanner.databinding.ActivityLoginScreenBinding
import com.google.firebase.auth.FirebaseAuth

class LoginScreen : AppCompatActivity() {
    private lateinit var binding: ActivityLoginScreenBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        binding=ActivityLoginScreenBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()


        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Login button clicked
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString()
            val password = binding.etLoginPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        // Go to main activity or dashboard
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // "Register here" text click
        binding.tvRegisterHere.setOnClickListener {
            startActivity(Intent(this, registerScreen::class.java))
        }

    }
}