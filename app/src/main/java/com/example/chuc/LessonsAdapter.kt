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
        val group: TextView = itemView.findViewById(R.id.lesson_group)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessonsList[position]
        
        holder.time.text = lesson.time
        holder.subject.text = lesson.shortTitle?.ifBlank { null } ?: lesson.title
        holder.subject.isSelected = true
        holder.teacher.text = lesson.teacher ?: "Не указан"
        holder.place.text = lesson.place
        
        // Отображаем группу если она есть (для расписания преподавателей)
        if (!lesson.group.isNullOrBlank()) {
            holder.group.text = lesson.group
            holder.group.visibility = View.VISIBLE
        } else {
            holder.group.visibility = View.GONE
        }
        
        // Отображаем тип занятия если он есть
        if (!lesson.lessonType.isNullOrBlank()) {
            holder.lessonType.text = lesson.lessonType
            holder.lessonType.visibility = View.VISIBLE

            val normalized = lesson.lessonType.lowercase()
            val context = holder.itemView.context
            when {
                normalized.contains("лекц") || normalized.contains("lecture") -> {
                    holder.lessonType.setBackgroundResource(R.drawable.type_background)
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
                }
                normalized.contains("практ") || normalized.contains("practice") -> {
                    holder.lessonType.setBackgroundColor(context.getColor(android.R.color.holo_orange_dark))
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
                }
                normalized.contains("лаб") || normalized.contains("lab") -> {
                    holder.lessonType.setBackgroundColor(context.getColor(android.R.color.holo_blue_dark))
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
                }
                normalized.contains("семин") || normalized.contains("seminar") -> {
                    holder.lessonType.setBackgroundColor(context.getColor(android.R.color.holo_purple))
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
                }
                normalized.contains("зач") -> {
                    holder.lessonType.setBackgroundColor(context.getColor(android.R.color.holo_green_dark))
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
                }
                normalized.contains("экзам") || normalized.contains("exam") -> {
                    holder.lessonType.setBackgroundColor(context.getColor(android.R.color.holo_red_dark))
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
                }
                normalized.contains("конс") -> {
                    holder.lessonType.setBackgroundColor(context.getColor(android.R.color.holo_blue_light))
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
                }
                else -> {
                    holder.lessonType.setBackgroundColor(context.getColor(android.R.color.darker_gray))
                    holder.lessonType.setTextColor(context.getColor(android.R.color.white))
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
