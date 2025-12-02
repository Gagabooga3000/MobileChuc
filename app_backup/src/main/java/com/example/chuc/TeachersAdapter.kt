package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeachersAdapter(
    private var teachersList: List<String>,
    private val onTeacherClick: (String) -> Unit
) : RecyclerView.Adapter<TeachersAdapter.TeacherViewHolder>() {
    
    class TeacherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teacherName: TextView = itemView.findViewById(R.id.teacher_name)
        val teacherDepartment: TextView = itemView.findViewById(R.id.teacher_department)
        val teacherPosition: TextView = itemView.findViewById(R.id.teacher_position)
        val teacherEmail: TextView = itemView.findViewById(R.id.teacher_email)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher, parent, false)
        return TeacherViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        val teacher = teachersList[position]
        
        holder.teacherName.text = teacher
        holder.teacherDepartment.text = "Преподаватель"
        holder.teacherPosition.text = "Нажмите для расписания"
        holder.teacherEmail.text = ""
        
        // Обработчик клика на преподавателя
        holder.itemView.setOnClickListener {
            onTeacherClick(teacher)
        }
    }
    
    override fun getItemCount(): Int = teachersList.size
    
    fun updateTeachers(newTeachersList: List<String>) {
        teachersList = newTeachersList
        notifyDataSetChanged()
    }
}

