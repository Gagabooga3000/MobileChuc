package com.example.chuc

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.chuc.data.local.SchedulePreferences
import com.example.chuc.data.local.ScheduleViewMode
import com.example.chuc.domain.model.UserRole
import com.example.chuc.presentation.theme.ThemeManager
import com.example.chuc.presentation.theme.ThemeMode
import com.example.chuc.presentation.viewmodel.ProfileViewModel
import com.example.chuc.schedule.UiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    
    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var schedulePreferences: SchedulePreferences
    
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var studentNameText: TextView
    private lateinit var studentGroupText: TextView
    private lateinit var studentCourseText: TextView
    private lateinit var studentFacultyText: TextView
    private lateinit var logoutButton: Button
    private lateinit var studentAvatar: ShapeableImageView
    private lateinit var openGradeBookButton: Button
    private lateinit var openJournalButton: Button
    private lateinit var themeToggleButton: MaterialButton
    private lateinit var scheduleViewToggle: com.google.android.material.button.MaterialButtonToggleGroup
    private lateinit var scheduleViewListButton: MaterialButton
    private lateinit var scheduleViewCalendarButton: MaterialButton
    private lateinit var appVersionText: TextView
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            loadImageFromUri(it)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализируем ViewModel
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        
        // Находим элементы UI
        studentNameText = view.findViewById(R.id.student_name)
        studentGroupText = view.findViewById(R.id.student_group)
        studentCourseText = view.findViewById(R.id.student_course)
        studentFacultyText = view.findViewById(R.id.student_faculty)
        logoutButton = view.findViewById(R.id.logout_button)
        studentAvatar = view.findViewById(R.id.student_avatar)
        openGradeBookButton = view.findViewById(R.id.open_grade_book_button)
        openJournalButton = view.findViewById(R.id.open_journal_button)
        themeToggleButton = view.findViewById(R.id.theme_toggle_button)
        scheduleViewToggle = view.findViewById(R.id.schedule_view_toggle)
        scheduleViewListButton = view.findViewById(R.id.schedule_view_list_button)
        scheduleViewCalendarButton = view.findViewById(R.id.schedule_view_calendar_button)
        appVersionText = view.findViewById(R.id.app_version_text)
        appVersionText.text = getString(R.string.app_version_label)
        
        // Загружаем сохраненный аватар если есть
        loadSavedAvatar()
        
        // Обработчик клика на аватар
        studentAvatar.setOnClickListener {
            openImagePicker()
        }
        
        // Наблюдаем за состоянием профиля
        profileViewModel.profileState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    // Показываем загрузку
                    studentNameText.text = "Загрузка..."
                    studentGroupText.visibility = View.VISIBLE
                    studentGroupText.text = "Загрузка..."
                    studentCourseText.visibility = View.VISIBLE
                    studentCourseText.text = ""
                    studentFacultyText.visibility = View.VISIBLE
                    studentFacultyText.text = ""
                }
                is UiState.Success -> {
                    val profile = state.data
                    studentNameText.text = profile.fullName

                    if (profile.role == UserRole.TEACHER) {
                        studentGroupText.visibility = View.GONE
                        studentCourseText.visibility = View.GONE
                        studentFacultyText.visibility = View.GONE
                    } else {
                        studentGroupText.visibility = View.VISIBLE
                        studentGroupText.text = if (profile.group.isNotBlank()) {
                            "Группа: ${profile.group}"
                        } else {
                            "Группа не определена"
                        }

                        if (profile.course.isNotBlank()) {
                            studentCourseText.visibility = View.VISIBLE
                            studentCourseText.text = profile.course
                        } else {
                            studentCourseText.visibility = View.GONE
                        }

                        if (profile.specialty.isNotBlank()) {
                            studentFacultyText.visibility = View.VISIBLE
                            studentFacultyText.text = "Специальность: ${profile.specialty}"
                        } else {
                            studentFacultyText.visibility = View.GONE
                        }
                    }
                }
                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    studentNameText.text = "Ошибка загрузки"
                    studentGroupText.visibility = View.VISIBLE
                    studentGroupText.text = "Ошибка загрузки"
                    studentCourseText.visibility = View.GONE
                    studentFacultyText.visibility = View.GONE
                }
            }
        })
        
        // Наблюдаем за состоянием выхода
        profileViewModel.logoutState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UiState.Loading -> {
                    logoutButton.isEnabled = false
                    logoutButton.text = "Выход..."
                }
                is UiState.Success -> {
                    Toast.makeText(context, "Вы вышли из системы", Toast.LENGTH_SHORT).show()
                    // Переходим на экран входа
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    logoutButton.isEnabled = true
                    logoutButton.text = "Выйти"
                }
            }
        })
        
        // Обработчик кнопки выхода
        logoutButton.setOnClickListener {
            profileViewModel.logout()
        }
        
        // Обработчик кнопки открытия зачетной книжки
        openGradeBookButton.setOnClickListener {
            openGradeBookFragment()
        }
        openJournalButton.setOnClickListener {
            openJournalFragment()
        }
        
        updateThemeToggleState(themeManager.getCurrentTheme())
        themeToggleButton.setOnClickListener {
            val mode = themeManager.toggleTheme()
            updateThemeToggleState(mode)
            Toast.makeText(requireContext(), themeChangeMessage(mode), Toast.LENGTH_SHORT).show()
        }

        // Настройка переключателя формата расписания
        val currentMode = schedulePreferences.getViewMode()
        val checkedId = when (currentMode) {
            ScheduleViewMode.LIST -> R.id.schedule_view_list_button
            ScheduleViewMode.CALENDAR -> R.id.schedule_view_calendar_button
        }
        scheduleViewToggle.check(checkedId)
        scheduleViewToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.schedule_view_calendar_button -> ScheduleViewMode.CALENDAR
                else -> ScheduleViewMode.LIST
            }
            schedulePreferences.setViewMode(mode)
        }
    }
    
    private fun updateThemeToggleState(themeMode: ThemeMode) {
        val buttonText = if (themeMode == ThemeMode.DARK) {
            "Включить светлую тему"
        } else {
            "Включить тёмную тему"
        }
        themeToggleButton.text = buttonText
    }
    
    private fun themeChangeMessage(themeMode: ThemeMode): String {
        return if (themeMode == ThemeMode.DARK) {
            "Тёмная тема включена"
        } else {
            "Светлая тема включена"
        }
    }
    
    private fun openGradeBookFragment() {
        val gradeBookFragment = GradeBookFragment.newInstance()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.menu_container, gradeBookFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openJournalFragment() {
        val fragment = JournalFragment.newInstance()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.menu_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                // Устанавливаем изображение в аватар
                studentAvatar.setImageBitmap(bitmap)
                
                // Сохраняем аватар
                saveAvatar(bitmap)
                
                Toast.makeText(context, "Аватар обновлен", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка загрузки изображения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveAvatar(bitmap: Bitmap) {
        try {
            val avatarFile = File(requireContext().filesDir, "avatar.jpg")
            val outputStream = FileOutputStream(avatarFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка сохранения аватара: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadSavedAvatar() {
        try {
            val avatarFile = File(requireContext().filesDir, "avatar.jpg")
            if (avatarFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(avatarFile.absolutePath)
                if (bitmap != null) {
                    studentAvatar.setImageBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки при загрузке сохраненного аватара
        }
    }
    
    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
}
