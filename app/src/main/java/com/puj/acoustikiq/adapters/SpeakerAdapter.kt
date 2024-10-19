package com.puj.acoustikiq.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.puj.acoustikiq.model.Speaker

class SpeakerAdapter(
    context: Context,
    resource: Int,
    private val speakers: List<Speaker>
) : ArrayAdapter<Speaker>(context, resource, speakers) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: TextView(context).apply {
            textSize = 16f
        }

        val speaker = getItem(position)
        (view as TextView).text = speaker?.model

        return view
    }

    @SuppressLint("SetTextI18n")
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: TextView(context).apply {
            textSize = 16f
        }

        val speaker = getItem(position)
        (view as TextView).text = "${speaker?.model} - ${speaker?.type}"

        return view
    }
}