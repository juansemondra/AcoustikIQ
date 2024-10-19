package com.puj.acoustikiq.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.puj.acoustikiq.databinding.ItemVenueBinding
import com.puj.acoustikiq.model.Venue

class VenueAdapter(
    private val venues: List<Venue>,
    private val onVenueClick: (Venue) -> Unit
) : RecyclerView.Adapter<VenueAdapter.VenueViewHolder>() {

    inner class VenueViewHolder(val binding: ItemVenueBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueViewHolder {
        val binding = ItemVenueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VenueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VenueViewHolder, position: Int) {
        val venue = venues[position]

        holder.binding.venueNameTextView.text = venue.name

        holder.itemView.setOnClickListener {
            onVenueClick(venue)
        }
    }

    override fun getItemCount(): Int = venues.size
}