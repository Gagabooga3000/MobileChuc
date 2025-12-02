package com.example.chuc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.chuc.R.id.imageview
import com.example.chuc.presentation.viewmodel.LoginViewModel
import com.example.chuc.schedule.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var loginViewModel: LoginViewModel
    
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

        // Инициализируем ViewModel
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        
        // Наблюдаем за состоянием авторизации
        loginViewModel.loginState.observe(this, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    logInButton.isEnabled = false
                    logInButton.text = "Вход..."
                }
                is UiState.Success -> {
                    Toast.makeText(this, "Успешно", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SecondActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is UiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    logInButton.isEnabled = true
                    logInButton.text = "Войти"
                }
            }
        })

        logInButton.setOnClickListener {
            val enteredLogin = loginEditText.text.toString().trim()
            val enteredPassword = passwordEditText.text.toString().trim()

            if (enteredLogin.isEmpty() || enteredPassword.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Используем реальную авторизацию через ViewModel
            loginViewModel.login(enteredLogin, enteredPassword)
        }
    }
}
