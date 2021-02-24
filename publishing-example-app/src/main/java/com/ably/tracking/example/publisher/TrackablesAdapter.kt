package com.ably.tracking.example.publisher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ably.tracking.publisher.Trackable
import kotlinx.android.synthetic.main.item_trackable.view.*

typealias OnTrackableItemClickedCallback = (Trackable) -> Unit

class TrackablesAdapter : RecyclerView.Adapter<TrackableViewHolder>() {
    var trackables: List<Trackable> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onItemClickedCallback: OnTrackableItemClickedCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackableViewHolder =
        TrackableViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_trackable, parent, false))

    override fun onBindViewHolder(holder: TrackableViewHolder, position: Int) {
        holder.bind(trackables[position], onItemClickedCallback)
    }

    override fun getItemCount(): Int = trackables.size
}

class TrackableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(trackable: Trackable, onItemClickedCallback: OnTrackableItemClickedCallback?) {
        itemView.trackableIdTextView.text = trackable.id
        onItemClickedCallback?.let { callback -> itemView.setOnClickListener { callback(trackable) } }

    }
}
