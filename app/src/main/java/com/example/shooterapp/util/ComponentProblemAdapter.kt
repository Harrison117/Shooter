package com.example.shooterapp.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shooterapp.databinding.RecyclerViewItemBinding

class ComponentProblemAdapter(private val problems: ArrayList<ComponentProblemItemViewModel>):
    RecyclerView.Adapter<ComponentProblemAdapter.ComponentProblemHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComponentProblemAdapter.ComponentProblemHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerViewItemBinding.inflate(inflater)
        return ComponentProblemHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ComponentProblemAdapter.ComponentProblemHolder,
        position: Int
    ) = holder.bind(problems[position])

    override fun getItemCount(): Int = problems.size

    inner class ComponentProblemHolder(val binding: RecyclerViewItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(problem: ComponentProblemItemViewModel) {
            binding.problem = problem
            binding.executePendingBindings()
        }
    }
}

data class ComponentProblemItemViewModel(val desc: String)
