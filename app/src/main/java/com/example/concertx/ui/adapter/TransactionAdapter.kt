package com.example.concertx.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.concertx.R
import com.example.concertx.model.TransactionItem

class TransactionAdapter(
    private var transactions: List<TransactionItem>,
    private val onUpdateClick: (TransactionItem, Int) -> Unit,
    private val onDeleteClick: (TransactionItem) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPoster: ImageView = itemView.findViewById(R.id.ivTxPoster)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTxTitle)
        val tvVenue: TextView = itemView.findViewById(R.id.tvTxVenue)
        val tvDate: TextView = itemView.findViewById(R.id.tvTxDate)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvTxQuantity)
        val etUpdateQuantity: EditText = itemView.findViewById(R.id.etUpdateQuantity)
        val btnUpdate: Button = itemView.findViewById(R.id.btnUpdateTx)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteTx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.concertTitle
        holder.tvVenue.text = "Venue: ${item.concertVenue}"
        holder.tvDate.text = "Date: ${item.concertDate}"
        holder.tvQuantity.text = "Quantity: ${item.quantity}"
        holder.etUpdateQuantity.setText("")
        holder.etUpdateQuantity.error = null

        val resId = context.resources.getIdentifier(
            item.concertImage.lowercase(),
            "drawable",
            context.packageName
        )
        if (resId != 0) {
            holder.ivPoster.setImageResource(resId)
        } else {
            holder.ivPoster.setImageResource(R.drawable.lany)
        }

        holder.btnUpdate.setOnClickListener {
            val input = holder.etUpdateQuantity.text.toString().trim()
            if (input.isEmpty()) {
                holder.etUpdateQuantity.error = "Quantity must be filled"
                holder.etUpdateQuantity.requestFocus()
                return@setOnClickListener
            }
            val parsedQty = input.toIntOrNull()
            if (parsedQty == null || parsedQty <= 0) {
                holder.etUpdateQuantity.error = "Quantity must be a number and more than 0"
                holder.etUpdateQuantity.requestFocus()
                return@setOnClickListener
            }
            holder.etUpdateQuantity.error = null
            onUpdateClick(item, parsedQty)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: List<TransactionItem>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}
