package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.R
import com.example.chuc.schedule.TeacherUiModel

class TeachersAdapter(
    private var teachersList: List<TeacherUiModel>,
    private val onTeacherClick: (TeacherUiModel) -> Unit
) : RecyclerView.Adapter<TeachersAdapter.TeacherViewHolder>() {
    
    class TeacherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teacherName: TextView = itemView.findViewById(R.id.teacher_name)
        val teacherStatus: TextView = itemView.findViewById(R.id.teacher_status)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher, parent, false)
        return TeacherViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        val teacher = teachersList[position]
        
        holder.teacherName.text = teacher.name
        val context = holder.itemView.context
        if (teacher.isActive) {
            holder.teacherStatus.text = context.getString(R.string.teacher_status_active)
            holder.teacherStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
        } else {
            holder.teacherStatus.text = context.getString(R.string.teacher_status_archived)
            holder.teacherStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        }
        
        // Обработчик клика на преподавателя
        holder.itemView.setOnClickListener {
            onTeacherClick(teacher)
        }
    }
    
    override fun getItemCount(): Int = teachersList.size
    
    fun updateTeachers(newTeachersList: List<TeacherUiModel>) {
        teachersList = newTeachersList
        notifyDataSetChanged()
    }
}

