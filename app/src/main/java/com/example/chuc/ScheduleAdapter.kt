package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.schedule.TimetableDay

class ScheduleAdapter : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {
    
    private var scheduleDays: List<TimetableDay> = emptyList()
    
    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTitle: TextView = itemView.findViewById(R.id.day_title)
        val lessonsRecyclerView: RecyclerView = itemView.findViewById(R.id.lessons_recycler_view)
        val lessonsCount: TextView = itemView.findViewById(R.id.lessons_count)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_day, parent, false)
        return ScheduleViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val day = scheduleDays[position]
        
        holder.dayTitle.text = day.dateLabel
        
        // Отображаем количество пар с корректными формами слова
        val lessonsCount = day.lessons.size
        holder.lessonsCount.text = when {
            lessonsCount == 0 -> "Нет пар"
            lessonsCount % 10 == 1 && lessonsCount % 100 != 11 -> "$lessonsCount пара"
            lessonsCount % 10 in 2..4 && lessonsCount % 100 !in 12..14 -> "$lessonsCount пары"
            else -> "$lessonsCount пар"
        }
        
        // Настройка RecyclerView для пар только если адаптер еще не установлен
        if (holder.lessonsRecyclerView.adapter == null) {
            val lessonsAdapter = LessonsAdapter(day.lessons)
            holder.lessonsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.context)
            holder.lessonsRecyclerView.adapter = lessonsAdapter
        } else {
            // Обновляем данные существующего адаптера
            (holder.lessonsRecyclerView.adapter as? LessonsAdapter)?.let { adapter ->
                adapter.updateLessons(day.lessons)
            }
        }
    }
    
    override fun getItemCount(): Int = scheduleDays.size
    
    fun updateSchedule(newScheduleDays: List<TimetableDay>) {
        scheduleDays = newScheduleDays
        notifyDataSetChanged()
    }
}
