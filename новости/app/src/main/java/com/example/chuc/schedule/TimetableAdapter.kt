package com.example.chuc.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.R

class TimetableAdapter(
    private var items: List<TimetableLesson>
) : RecyclerView.Adapter<TimetableAdapter.LessonViewHolder>() {

    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val time: TextView = itemView.findViewById(R.id.lesson_time)
        val title: TextView = itemView.findViewById(R.id.lesson_title)
        val place: TextView = itemView.findViewById(R.id.lesson_place)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timetable_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val item = items[position]
        holder.time.text = item.time
        holder.title.text = item.title
        holder.place.text = item.place
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<TimetableLesson>) {
        items = newItems
        notifyDataSetChanged()
    }
}


