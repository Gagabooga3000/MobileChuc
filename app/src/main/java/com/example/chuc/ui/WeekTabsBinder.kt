package com.example.chuc.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.chuc.R
import com.example.chuc.schedule.TimetableDay
import com.google.android.material.tabs.TabLayout

private val WEEKDAY_LABELS = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ")
private val DAY_NUMBER_REGEX = Regex("(\\d{1,2})")

/**
 * Привязывает данные недели к TabLayout и отображает только учебные дни (ПН-СБ).
 */
fun TabLayout.bindWeekTabs(
    days: List<TimetableDay>,
    inflater: LayoutInflater,
    onDaySelected: (TimetableDay) -> Unit
) {
    val displayDays = days.take(WEEKDAY_LABELS.size)
    if (displayDays.isEmpty()) {
        visibility = View.GONE
        clearOnTabSelectedListeners()
        removeAllTabs()
        return
    }

    visibility = View.VISIBLE
    removeAllTabs()

    val weekdayColor = ContextCompat.getColor(context, R.color.calendar_weekday_text)

    displayDays.forEachIndexed { index, day ->
        val tab = newTab()
        val view = inflater.inflate(R.layout.item_calendar_day, null)
        val dowView = view.findViewById<TextView>(R.id.day_of_week_text)
        val domView = view.findViewById<TextView>(R.id.day_of_month_text)

        dowView.text = WEEKDAY_LABELS.getOrElse(index) { WEEKDAY_LABELS.last() }
        dowView.setTextColor(weekdayColor)
        domView.text = extractDayNumber(day.dateLabel)

        tab.customView = view
        addTab(tab, index == 0)
        updateTabSelection(tab, index == 0)
    }

    onDaySelected(displayDays.first())

    clearOnTabSelectedListeners()
    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            val position = tab.position.coerceIn(displayDays.indices)
            onDaySelected(displayDays[position])
            updateTabSelection(tab, true)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            updateTabSelection(tab, false)
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
            onTabSelected(tab)
        }
    })
}

private fun TabLayout.updateTabSelection(tab: TabLayout.Tab, isSelected: Boolean) {
    val tabView = tab.customView ?: return
    val domView = tabView.findViewById<TextView>(R.id.day_of_month_text)

    val numberSelectedColor = ContextCompat.getColor(context, R.color.calendar_day_number_selected)
    val numberDefaultColor = ContextCompat.getColor(context, R.color.calendar_day_number)

    domView.isSelected = isSelected
    domView.setTextColor(if (isSelected) numberSelectedColor else numberDefaultColor)

    tabView.isSelected = isSelected
}

private fun extractDayNumber(label: String?): String {
    if (label.isNullOrBlank()) return ""
    return DAY_NUMBER_REGEX.find(label)?.value ?: ""
}

