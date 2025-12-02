package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.schedule.GradeBookEntry

class GradeBookAdapter(
    private var entries: List<GradeBookEntry>
) : RecyclerView.Adapter<GradeBookAdapter.GradeBookViewHolder>() {

    fun updateEntries(newEntries: List<GradeBookEntry>) {
        android.util.Log.d("GradeBookAdapter", "updateEntries called with ${newEntries.size} entries")
        entries = newEntries
        android.util.Log.d("GradeBookAdapter", "Entries updated, calling notifyDataSetChanged")
        notifyDataSetChanged()
        android.util.Log.d("GradeBookAdapter", "notifyDataSetChanged called, itemCount: ${itemCount}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade_book_entry, parent, false)
        return GradeBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeBookViewHolder, position: Int) {
        holder.bind(entries[position], position)
    }

    override fun getItemCount(): Int = entries.size

    class GradeBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberText: TextView = itemView.findViewById(R.id.number_text)
        private val disciplineText: TextView = itemView.findViewById(R.id.discipline_text)
        private val course1Sem1Text: TextView = itemView.findViewById(R.id.course1_sem1_text)
        private val course1Sem2Text: TextView = itemView.findViewById(R.id.course1_sem2_text)
        private val course2Sem3Text: TextView = itemView.findViewById(R.id.course2_sem3_text)
        private val course2Sem4Text: TextView = itemView.findViewById(R.id.course2_sem4_text)
        private val course3Sem5Text: TextView = itemView.findViewById(R.id.course3_sem5_text)
        private val course3Sem6Text: TextView = itemView.findViewById(R.id.course3_sem6_text)
        private val course4Sem7Text: TextView? = itemView.findViewById(R.id.course4_sem7_text)
        private val course4Sem8Text: TextView? = itemView.findViewById(R.id.course4_sem8_text)

        fun bind(entry: GradeBookEntry, position: Int) {
            android.util.Log.d("GradeBookAdapter", "Binding entry: ${entry.discipline}")
            numberText.text = "${position + 1}."
            disciplineText.text = entry.discipline
            course1Sem1Text.text = entry.course1Sem1 ?: "-"
            course1Sem2Text.text = entry.course1Sem2 ?: "-"
            course2Sem3Text.text = entry.course2Sem3 ?: "-"
            course2Sem4Text.text = entry.course2Sem4 ?: "-"
            course3Sem5Text.text = entry.course3Sem5 ?: "-"
            course3Sem6Text.text = entry.course3Sem6 ?: "-"
            course4Sem7Text?.text = entry.course4Sem7 ?: "-"
            course4Sem8Text?.text = entry.course4Sem8 ?: "-"
        }
    }
}

