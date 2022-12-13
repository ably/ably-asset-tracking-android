package com.ably.tracking.example.publisher

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ably.tracking.publisher.Trackable
import kotlinx.android.synthetic.main.item_trackable.view.*

typealias OnTrackableItemClickedCallback = (Trackable) -> Unit

class TrackablesAdapter : RecyclerView.Adapter<TrackableViewHolder>() {
    var trackables: List<Trackable> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value

            // If we don't suppress the lint rule then we get this at compile time for this line:
            //   Error: It will always be more efficient to use more specific change events if you can. Rely on notifyDataSetChanged as a last resort. [NotifyDataSetChanged]
            // For our use case, this performance cost is unlikely to be a problem. We can always revisit in future if it becomes one.
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
