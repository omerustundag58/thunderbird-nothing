package com.fsck.k9.ui.localai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R

class CategorySummaryAdapter(
    private val onClick: (EmailCategory) -> Unit,
) : RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder>() {
    private var items: List<CategoryOverview> = emptyList()

    fun submit(newItems: List<CategoryOverview>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.local_ai_category_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.categoryTitle)
        private val count: TextView = view.findViewById(R.id.categoryCount)
        private val samples: TextView = view.findViewById(R.id.categorySamples)
        private val context = view.context

        fun bind(overview: CategoryOverview, onClick: (EmailCategory) -> Unit) {
            title.text = context.getString(overview.category.titleRes)
            count.text = overview.count.toString()
            samples.text = if (overview.samples.isEmpty()) {
                context.getString(R.string.local_ai_category_no_samples)
            } else {
                overview.samples.joinToString(separator = "\n") { sample ->
                    val trimmedSubject = sample.subject.trim().ifEmpty {
                        context.getString(R.string.local_ai_no_subject)
                    }
                    "• $trimmedSubject"
                }
            }

            itemView.setOnClickListener {
                onClick(overview.category)
            }
        }
    }
}
