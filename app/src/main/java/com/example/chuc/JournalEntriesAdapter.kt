package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.presentation.viewmodel.JournalRowUiModel

class JournalEntriesAdapter(
    private var entries: List<JournalRowUiModel>
) : RecyclerView.Adapter<JournalEntriesAdapter.JournalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_row, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    fun submitList(newEntries: List<JournalRowUiModel>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.journal_date)
        private val subjectText: TextView = itemView.findViewById(R.id.journal_subject)
        private val teacherText: TextView = itemView.findViewById(R.id.journal_teacher)
        private val timeText: TextView = itemView.findViewById(R.id.journal_time)
        private val gradeText: TextView = itemView.findViewById(R.id.journal_grade)

        fun bind(item: JournalRowUiModel) {
            val context = itemView.context
            dateText.text = item.displayDate
            subjectText.text = item.subject
            teacherText.text = item.teacher
            timeText.text = item.timeAndPlace
            gradeText.text = item.gradeLabel

            if (item.isPositive) {
                gradeText.setBackgroundResource(R.drawable.type_background)
                gradeText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else {
                gradeText.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                gradeText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
        }
    }
}

