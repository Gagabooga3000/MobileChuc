package com.example.chuc

import android.animation.ValueAnimator
import android.content.Intent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.chuc.R.id.imageview
import com.example.chuc.data.mediator.MediatorRepository
import com.example.chuc.data.repository.AuthRepository
import com.example.chuc.domain.model.UserProfile
import com.example.chuc.domain.model.UserProfileStorage
import com.example.chuc.domain.model.UserRole
import com.example.chuc.presentation.viewmodel.LoginViewModel
import com.example.chuc.schedule.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    @Inject lateinit var mediatorRepository: MediatorRepository
    @Inject lateinit var authRepository: AuthRepository

    private var autoLoginTriggered = false
    private var heroAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val logInButton: Button = findViewById(R.id.logIn)
        val loginEditText: EditText = findViewById(R.id.login)
        val passwordEditText: EditText = findViewById(R.id.password)
        val loginCard: View = findViewById(R.id.login_container)
        val heroImage: ImageView = findViewById(imageview)
        val loginContainer: View = findViewById(R.id.login_container)

        Glide.with(this)
            .load("https://example.com/image.jpg")
            .into(findViewById(imageview))

        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        applySystemInsets(loginCard)
        startHeroAnimations(heroImage, loginCard)

        loginViewModel.loginState.observe(this, Observer { state ->
            Log.d("MainActivity", "Login state changed: $state")

            when (state) {
                is UiState.Loading -> {
                    logInButton.isEnabled = false
                    logInButton.text = "Вход..."
                }
                is UiState.Success -> {
                    Toast.makeText(this, "Успешно", Toast.LENGTH_SHORT).show()

                    val resolvedLogin = state.data.user?.username
                        ?: loginEditText.text.toString().trim()

                    lifecycleScope.launch {
                        val cachedProfile = UserProfileStorage.load(this@MainActivity)
                        val roleHint = determineRole(resolvedLogin)

                        val detectedProfile = detectProfileSafely(roleHint)
                            ?: detectProfileSafely(roleHint.opposite())
                            ?: cachedProfile?.also {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Использую сохранённый профиль. Перезайдите позже для обновления.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        val profile = detectedProfile ?: UserProfile(
                            role = roleHint,
                            groupName = cachedProfile?.groupName,
                            teacherName = cachedProfile?.teacherName
                        )

                        UserProfileStorage.save(this@MainActivity, profile)

                        val intent = Intent(this@MainActivity, SecondActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                is UiState.Error -> {
                    Toast.makeText(this, state.message ?: "Ошибка авторизации", Toast.LENGTH_LONG).show()
                    logInButton.isEnabled = true
                    logInButton.text = "Войти"
                }
            }
        })

        loginViewModel.isLoading.observe(this, Observer { isLoading ->
            logInButton.isEnabled = !isLoading
            logInButton.text = if (isLoading) "Вход..." else "Войти"
        })

        logInButton.setOnClickListener {
            val enteredLogin = loginEditText.text.toString().trim()
            val enteredPassword = passwordEditText.text.toString().trim()

            if (enteredLogin.isEmpty() || enteredPassword.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginViewModel.login(enteredLogin, enteredPassword)
        }

        maybeAutoLogin()
    }

    private fun maybeAutoLogin() {
        if (autoLoginTriggered) return

        lifecycleScope.launch {
            if (authRepository.shouldForceReset()) {
                authRepository.logout()
                Toast.makeText(
                    this@MainActivity,
                    "Срок действия сессии истёк. Введите данные снова.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            // Если есть сохранённые данные и нет интернета — пробуем сразу зайти с кэшированным профилем
            if (authRepository.hasCredentials() && !hasNetworkConnection()) {
                val cachedProfile = UserProfileStorage.load(this@MainActivity)
                if (cachedProfile != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Нет подключения к сети. Использую сохранённый профиль.",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this@MainActivity, SecondActivity::class.java))
                    finish()
                    return@launch
                }
            }

            if (authRepository.hasCredentials()) {
                autoLoginTriggered = true
                loginViewModel.autoLogin()
            }
        }
    }

    private fun determineRole(login: String): UserRole {
        return if (login.any { it.isDigit() }) {
            UserRole.STUDENT
        } else {
            UserRole.TEACHER
        }
    }

    private fun applySystemInsets(target: View) {
        val defaultBottomPadding = target.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = defaultBottomPadding + systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(target)
    }

    private fun startHeroAnimations(hero: ImageView, card: View) {
        heroAnimator?.cancel()
        heroAnimator = ValueAnimator.ofFloat(1f, 1.08f).apply {
            duration = 12000L
            interpolator = LinearInterpolator()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                hero.scaleX = value
                hero.scaleY = value
            }
            start()
        }

        card.alpha = 0f
        card.translationY = 80f
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(650L)
            .setStartDelay(150L)
            .setInterpolator(LinearInterpolator())
            .start()
    }

    private suspend fun detectProfileSafely(role: UserRole): UserProfile? {
        val result = mediatorRepository.detectCurrentProfile(role)
        return result.onFailure {
            Log.e("MainActivity", "detectCurrentProfile failed for $role: ${it.message}")
        }.getOrNull()
    }

    private fun UserRole.opposite(): UserRole {
        return if (this == UserRole.STUDENT) UserRole.TEACHER else UserRole.STUDENT
    }

    private fun hasNetworkConnection(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        heroAnimator?.cancel()
        super.onDestroy()
    }
}
