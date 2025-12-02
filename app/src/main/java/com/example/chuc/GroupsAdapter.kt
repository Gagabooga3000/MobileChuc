package com.example.chuc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chuc.R
import com.example.chuc.schedule.GroupUiModel

class GroupsAdapter(
    private var groupsList: List<GroupUiModel>,
    private val onGroupClick: (GroupUiModel) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {
    
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(R.id.group_name)
        val groupMeta: TextView = itemView.findViewById(R.id.group_meta)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groupsList[position]
        holder.groupName.text = group.code
        val startYear = group.startYear
        if (startYear != null) {
            holder.groupMeta.visibility = View.VISIBLE
            holder.groupMeta.text = holder.itemView.context.getString(R.string.group_start_year, startYear)
        } else {
            holder.groupMeta.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onGroupClick(group)
        }
    }
    
    override fun getItemCount(): Int = groupsList.size
    
    fun updateGroups(newGroupsList: List<GroupUiModel>) {
        groupsList = newGroupsList
        notifyDataSetChanged()
    }
}

