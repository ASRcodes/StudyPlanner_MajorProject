package com.example.smartplanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()

        // Delay for splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is logged in
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is signed in, go to HomeActivity
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
            } else {
                // User not signed in, go to Register screen
                startActivity(Intent(this@MainActivity, registerScreen::class.java))
            }
            finish()
        }, 3000) // Adjust time if needed
    }
}