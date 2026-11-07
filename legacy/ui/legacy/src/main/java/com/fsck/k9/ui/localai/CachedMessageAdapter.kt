package com.fsck.k9.ui.localai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import java.text.DateFormat
import java.util.Date

class CachedMessageAdapter : RecyclerView.Adapter<CachedMessageAdapter.ViewHolder>() {
    private var items: List<CachedCategory> = emptyList()

    fun submit(newItems: List<CachedCategory>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.local_ai_message_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val sender: TextView = view.findViewById(R.id.messageSender)
        private val subject: TextView = view.findViewById(R.id.messageSubject)
        private val date: TextView = view.findViewById(R.id.messageDate)
        private val context = view.context

        fun bind(item: CachedCategory) {
            sender.text = item.sender.ifBlank { context.getString(R.string.local_ai_unknown_sender) }
            subject.text = item.subject.ifBlank { context.getString(R.string.local_ai_no_subject) }
            date.text = if (item.date > 0L) {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.date))
            } else {
                ""
            }
        }
    }
}
