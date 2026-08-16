package com.airi.odslm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airi.odslm.R
import com.airi.odslm.data.MessageRole
import com.airi.odslm.viewmodel.ChatMessage

/**
 * RecyclerView adapter for the chat screen.
 *
 * Uses [ListAdapter] with [DiffUtil] for efficient list updates.
 * Two view types: USER bubble (right) and ASSISTANT bubble (left).
 *
 * MVVM boundary: adapter only renders data, no business logic here.
 */
class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_MODEL = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).role == MessageRole.USER) VIEW_TYPE_USER else VIEW_TYPE_MODEL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            ChatViewHolder.User(inflater.inflate(R.layout.item_chat_user, parent, false))
        } else {
            ChatViewHolder.Model(inflater.inflate(R.layout.item_chat_model, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** Sealed class for the two ViewHolder types — eliminates unchecked cast. */
    sealed class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(message: ChatMessage)

        class User(view: View) : ChatViewHolder(view) {
            private val textMessage: TextView = view.findViewById(R.id.textMessage)
            private val imageAttachment: ImageView = view.findViewById(R.id.imageAttachment)

            override fun bind(message: ChatMessage) {
                textMessage.text = message.content
                // ponytail: image loading via URI not wired yet — Task 7 handles full image display
                imageAttachment.visibility =
                    if (message.imagePath != null) View.VISIBLE else View.GONE
            }
        }

        class Model(view: View) : ChatViewHolder(view) {
            private val textMessage: TextView = view.findViewById(R.id.textMessage)

            override fun bind(message: ChatMessage) {
                textMessage.text = message.content
            }
        }
    }

    /** DiffUtil callback — compares by stable [ChatMessage.id], content for change detection. */
    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem == newItem
    }
}
