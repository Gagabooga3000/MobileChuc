package com.example.chuc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.chuc.data.local.ParserPreferences
import com.example.chuc.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    
    @Inject
    lateinit var parserPreferences: ParserPreferences
    
    private lateinit var parserTypeRadioGroup: RadioGroup
    private lateinit var pythonServerSwitch: Switch
    private lateinit var usePythonButton: Button
    private lateinit var settingsViewModel: SettingsViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        
        parserTypeRadioGroup = view.findViewById(R.id.parser_type_radio_group)
        pythonServerSwitch = view.findViewById(R.id.python_server_switch)
        usePythonButton = view.findViewById(R.id.btn_use_python)
        
        setupUI()
        setupListeners()
    }
    
    private fun setupUI() {
        // Устанавливаем текущие значения
        val currentParserType = parserPreferences.getParserType()
        val pythonServerEnabled = parserPreferences.isPythonServerEnabled()
        
        when (currentParserType) {
            ParserPreferences.PARSER_TYPE_NATIVE -> {
                parserTypeRadioGroup.check(R.id.radio_native)
            }
            ParserPreferences.PARSER_TYPE_PYTHON -> {
                parserTypeRadioGroup.check(R.id.radio_python)
            }
            ParserPreferences.PARSER_TYPE_AUTO -> {
                parserTypeRadioGroup.check(R.id.radio_auto)
            }
        }
        
        pythonServerSwitch.isChecked = pythonServerEnabled
    }
    
    private fun setupListeners() {
        parserTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val parserType = when (checkedId) {
                R.id.radio_native -> ParserPreferences.PARSER_TYPE_NATIVE
                R.id.radio_python -> ParserPreferences.PARSER_TYPE_PYTHON
                R.id.radio_auto -> ParserPreferences.PARSER_TYPE_AUTO
                else -> ParserPreferences.PARSER_TYPE_AUTO
            }
            
            parserPreferences.setParserType(parserType)
            Toast.makeText(context, "Тип парсера изменен", Toast.LENGTH_SHORT).show()
        }
        
        pythonServerSwitch.setOnCheckedChangeListener { _, isChecked ->
            parserPreferences.setPythonServerEnabled(isChecked)
            Toast.makeText(context, "Python сервер ${if (isChecked) "включен" else "выключен"}", Toast.LENGTH_SHORT).show()
        }
        
        usePythonButton.setOnClickListener {
            // Переключаем на Python парсер
            parserPreferences.setParserType(ParserPreferences.PARSER_TYPE_PYTHON)
            parserPreferences.setPythonServerEnabled(true)
            
            // Обновляем UI
            parserTypeRadioGroup.check(R.id.radio_python)
            pythonServerSwitch.isChecked = true
            
            Toast.makeText(context, "Переключено на Python парсер! Теперь расписание работает без авторизации.", Toast.LENGTH_LONG).show()
        }
    }
    
    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
