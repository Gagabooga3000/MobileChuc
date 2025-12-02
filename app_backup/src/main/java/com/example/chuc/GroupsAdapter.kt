package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupsAdapter(
    private var groupsList: List<String>,
    private val onGroupClick: (String) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {
    
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(R.id.group_name)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groupsList[position]
        holder.groupName.text = group
        
        holder.itemView.setOnClickListener {
            onGroupClick(group)
        }
    }
    
    override fun getItemCount(): Int = groupsList.size
    
    fun updateGroups(newGroupsList: List<String>) {
        groupsList = newGroupsList
        notifyDataSetChanged()
    }
}

