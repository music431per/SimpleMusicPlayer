package com.example.simplemusicplayer.ui

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.simplemusicplayer.R
import com.example.simplemusicplayer.model.Track

class ListTrackAdapter(val list: List<Track>) : RecyclerView.Adapter<ListTrackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.track = list[position]
        holder.trackTextView.text = list[position].title
        holder.artistTextView.text = list[position].artist
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var track: Track
        val trackTextView = view.findViewById<View>(R.id.music_name) as TextView
        val artistTextView = view.findViewById<View>(R.id.artist_name) as TextView

        init {
            itemView.setOnClickListener { v ->
                val context = v.context
                val intent = Intent(context, MusicPlayerActivity::class.java)
                intent.putExtra("id",track.id)
                intent.putExtra("position",layoutPosition)
                context.startActivity(intent)
            }
        }

    }
}