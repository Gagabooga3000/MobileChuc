package com.example.chuc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.example.chuc.R.id.imageview

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val logInButton: Button = findViewById(R.id.logIn)
        val loginEditText: EditText = findViewById(R.id.login)
        val passwordEditText: EditText = findViewById(R.id.password)

        Glide.with(this)
            .load("https://example.com/image.jpg")
            .into(findViewById(imageview))


        val correctLogin = "qwe"
        val correctPassword = "asd"

        logInButton.setOnClickListener {
            val enteredLogin = loginEditText.text.toString()
            val enteredPassword = passwordEditText.text.toString()

            if (enteredLogin == correctLogin && enteredPassword == correctPassword) {
                Toast.makeText(this, "Успешно", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SecondActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Неверно", Toast.LENGTH_SHORT).show()
            }

        }
    }
}
