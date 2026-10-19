package com.example.concertx.model

data class TransactionItem(
    val transactionId: Int,
    val userId: Int,
    val concertId: Int,
    val quantity: Int,
    val concertTitle: String,
    val concertVenue: String,
    val concertDate: String,
    val concertImage: String
)
