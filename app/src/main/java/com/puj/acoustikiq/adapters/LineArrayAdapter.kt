package com.puj.acoustikiq.adapters

import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.puj.acoustikiq.databinding.ItemLineArrayBinding
import com.puj.acoustikiq.model.LineArray

class LineArrayAdapter(private val lineArrays: List<LineArray>) :
    RecyclerView.Adapter<LineArrayAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemLineArrayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLineArrayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lineArray = lineArrays[position]
        holder.binding.typeTextView.text = "Type: ${lineArray.type}"
        holder.binding.systemTextView.text = "System: ${lineArray.system.model} (${lineArray.system.type})"
        holder.binding.quantityTextView.text = "Quantity: ${lineArray.quantity}"
        holder.binding.locationTextView.text = "Location: ${lineArray.location.latitude}, ${lineArray.location.longitude}, ${lineArray.location.altitude}"
        holder.binding.delayTextView.text = "Delay: ${lineArray.delay}"
        holder.binding.calibratedFreqTextView.text = "Calibrated Freq: ${lineArray.calibratedFreq}"
        holder.binding.calibratedPhaseTextView.text = "Calibrated Phase: ${lineArray.calibratedPhase}"
    }

    override fun getItemCount(): Int = lineArrays.size
}