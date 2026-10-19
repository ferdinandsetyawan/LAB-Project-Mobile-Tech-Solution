package com.example.concertx.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.concertx.R
import com.example.concertx.database.DatabaseHelper
import com.example.concertx.model.TransactionItem
import com.example.concertx.ui.adapter.TransactionAdapter
import com.example.concertx.utils.SessionManager

class TransactionFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyTransactions: TextView
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        rvTransactions = view.findViewById(R.id.rvTransactions)
        tvEmptyTransactions = view.findViewById(R.id.tvEmptyTransactions)

        transactionAdapter = TransactionAdapter(
            transactions = emptyList(),
            onUpdateClick = { item, newQuantity ->
                handleUpdateTransaction(item, newQuantity)
            },
            onDeleteClick = { item ->
                showDeleteConfirmationDialog(item)
            }
        )

        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = transactionAdapter

        loadUserTransactions()
    }

    override fun onResume() {
        super.onResume()
        loadUserTransactions()
    }

    private fun loadUserTransactions() {
        val userId = sessionManager.getLoggedInUserId()
        val transactions = dbHelper.getTransactionsByUser(userId)

        if (transactions.isEmpty()) {
            rvTransactions.visibility = View.GONE
            tvEmptyTransactions.visibility = View.VISIBLE
        } else {
            rvTransactions.visibility = View.VISIBLE
            tvEmptyTransactions.visibility = View.GONE
            transactionAdapter.updateData(transactions)
        }
    }

    private fun handleUpdateTransaction(item: TransactionItem, newQuantity: Int) {
        val updated = dbHelper.updateTransactionQuantity(item.transactionId, newQuantity)
        if (updated) {
            Toast.makeText(
                requireContext(),
                "Transaction quantity updated to $newQuantity",
                Toast.LENGTH_SHORT
            ).show()
            loadUserTransactions()
        } else {
            Toast.makeText(
                requireContext(),
                "Failed to update transaction",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDeleteConfirmationDialog(item: TransactionItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete your ticket transaction for \"${item.concertTitle}\"?")
            .setPositiveButton("Delete") { _, _ ->
                val deleted = dbHelper.deleteTransaction(item.transactionId)
                if (deleted) {
                    Toast.makeText(
                        requireContext(),
                        "Transaction deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadUserTransactions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
