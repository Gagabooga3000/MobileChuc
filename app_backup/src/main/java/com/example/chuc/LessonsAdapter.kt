package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.schedule.TimetableLesson

class LessonsAdapter(
    private var lessonsList: List<TimetableLesson>
) : RecyclerView.Adapter<LessonsAdapter.LessonViewHolder>() {
    
    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val time: TextView = itemView.findViewById(R.id.lesson_time)
        val subject: TextView = itemView.findViewById(R.id.lesson_subject)
        val teacher: TextView = itemView.findViewById(R.id.lesson_teacher)
        val place: TextView = itemView.findViewById(R.id.lesson_place)
        val lessonType: TextView = itemView.findViewById(R.id.lesson_type)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessonsList[position]
        
        holder.time.text = lesson.time
        holder.subject.text = lesson.title
        holder.teacher.text = lesson.teacher ?: "Не указан"
        holder.place.text = lesson.place
        
        // Отображаем тип занятия если он есть
        if (!lesson.lessonType.isNullOrBlank()) {
            holder.lessonType.text = lesson.lessonType
            holder.lessonType.visibility = View.VISIBLE
            
            // Устанавливаем цвет в зависимости от типа занятия
            when (lesson.lessonType.lowercase()) {
                "лекция", "lecture" -> {
                    holder.lessonType.setBackgroundResource(R.drawable.type_background)
                    holder.lessonType.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                }
                "практика", "practice", "лабораторная", "lab" -> {
                    holder.lessonType.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                    holder.lessonType.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                }
                "семинар", "seminar" -> {
                    holder.lessonType.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_purple))
                    holder.lessonType.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                }
                else -> {
                    holder.lessonType.setBackgroundColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.lessonType.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                }
            }
        } else {
            holder.lessonType.visibility = View.GONE
        }
    }
    
    override fun getItemCount(): Int = lessonsList.size
    
    fun updateLessons(newLessonsList: List<TimetableLesson>) {
        lessonsList = newLessonsList
        notifyDataSetChanged()
    }
}
