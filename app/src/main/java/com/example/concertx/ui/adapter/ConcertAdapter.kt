package com.example.concertx.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.concertx.R
import com.example.concertx.model.Concert

class ConcertAdapter(
    private var concerts: List<Concert>,
    private val onItemClick: (Concert) -> Unit
) : RecyclerView.Adapter<ConcertAdapter.ConcertViewHolder>() {

    inner class ConcertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPoster: ImageView = itemView.findViewById(R.id.ivConcertPoster)
        val tvTitle: TextView = itemView.findViewById(R.id.tvConcertTitle)
        val tvVenue: TextView = itemView.findViewById(R.id.tvConcertVenue)
        val tvCity: TextView = itemView.findViewById(R.id.tvConcertCity)
        val tvDate: TextView = itemView.findViewById(R.id.tvConcertDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConcertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_concert, parent, false)
        return ConcertViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConcertViewHolder, position: Int) {
        val concert = concerts[position]
        val context = holder.itemView.context

        holder.tvTitle.text = concert.title
        holder.tvVenue.text = "Venue: ${concert.venue}"
        holder.tvCity.text = "City: ${concert.city}"
        holder.tvDate.text = "Date: ${concert.date}"

        val resId = context.resources.getIdentifier(
            concert.image.lowercase(),
            "drawable",
            context.packageName
        )
        if (resId != 0) {
            holder.ivPoster.setImageResource(resId)
        } else {
            holder.ivPoster.setImageResource(R.drawable.lany)
        }

        holder.itemView.setOnClickListener {
            onItemClick(concert)
        }
    }

    override fun getItemCount(): Int = concerts.size

    fun updateData(newConcerts: List<Concert>) {
        concerts = newConcerts
        notifyDataSetChanged()
    }
}
