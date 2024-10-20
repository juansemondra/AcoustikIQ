package com.puj.acoustikiq.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.storage
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.MessageAdapterBinding
import com.puj.acoustikiq.model.Message
import java.text.DateFormat


class MessagesAdapter(private var positionList: ArrayList<Message>) :
    RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {
    private val storage = Firebase.storage

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: MessageAdapterBinding

        init {
            binding = MessageAdapterBinding.bind(itemView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.message_adapter, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return positionList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = positionList[position]
        val scale: Float = holder.binding.root.context.resources.displayMetrics.density
        if (msg.from == Firebase.auth.currentUser?.email) {
            val newLayoutParams = holder.binding.messageCard.layoutParams as ViewGroup.MarginLayoutParams
            newLayoutParams.marginStart = (35 * scale + 0.5f).toInt()
            newLayoutParams.marginEnd = (5 * scale + 0.5f).toInt()
            holder.binding.messageCard.layoutParams = newLayoutParams
        }else {
            val newLayoutParams = holder.binding.messageCard.layoutParams as ViewGroup.MarginLayoutParams
            newLayoutParams.marginStart = (5 * scale + 0.5f).toInt()
            newLayoutParams.marginEnd = (35 * scale + 0.5f).toInt()
            holder.binding.messageCard.layoutParams = newLayoutParams
        }
        holder.binding.msgName.text = msg.from
        holder.binding.msgContent.text = msg.content
        val refProfileImg = storage.reference.child("users/${msg.fromId}/profile.jpg")
        Glide.with(holder.binding.root)
            .load(refProfileImg)
            .centerCrop()
            .placeholder(R.drawable.baseline_face_24)
            .into(holder.binding.msgProfileImage)
        if (msg.img != "") {
            val refImg = storage.reference.child(msg.img)
            holder.binding.msgImage.visibility = View.VISIBLE
            Glide.with(holder.binding.root)
                .load(refImg)
                .centerCrop()
                .placeholder(R.drawable.baseline_image_24)
                .into(holder.binding.msgImage)
        } else holder.binding.msgImage.visibility = View.GONE
        holder.binding.msgDate.text = DateFormat.getDateTimeInstance().format(msg.timestamp)
    }
}