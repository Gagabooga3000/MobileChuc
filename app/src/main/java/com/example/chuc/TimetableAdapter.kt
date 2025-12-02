package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.schedule.TimetableDay
import com.example.chuc.schedule.TimetableLesson

class TimetableAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_LESSON = 1
    }

    private val items: MutableList<Item> = mutableListOf()

    sealed class Item {
        data class Header(val title: String) : Item()
        data class Lesson(val value: TimetableLesson) : Item()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Item.Header -> TYPE_HEADER
            is Item.Lesson -> TYPE_LESSON
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timetable_lesson, parent, false)
            LessonViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.Header -> (holder as HeaderViewHolder).bind(item)
            is Item.Lesson -> (holder as LessonViewHolder).bind(item.value)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitDays(days: List<TimetableDay>) {
        items.clear()
        for (day in days) {
            items.add(Item.Header(day.dateLabel))
            day.lessons.forEach { lesson ->
                items.add(Item.Lesson(lesson))
            }
        }
        notifyDataSetChanged()
    }

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(android.R.id.text1)
        fun bind(item: Item.Header) {
            titleView.text = item.title
        }
    }

    private class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lessonTime: TextView = itemView.findViewById(R.id.lesson_time)
        private val lessonTitle: TextView = itemView.findViewById(R.id.lesson_title)
        private val lessonPlace: TextView = itemView.findViewById(R.id.lesson_place)
        fun bind(lesson: TimetableLesson) {
            lessonTime.text = lesson.time
            lessonTitle.text = lesson.title
            lessonPlace.text = lesson.place
        }
    }
}
