package com.example.concertx.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.concertx.model.Concert
import com.example.concertx.model.TransactionItem
import com.example.concertx.model.User

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "concertx.db"
        private const val DATABASE_VERSION = 1

        // Table Users
        const val TABLE_USERS = "Users"
        const val COL_USER_ID = "UserID"
        const val COL_EMAIL = "Email"
        const val COL_USERNAME = "Username"
        const val COL_PHONE = "PhoneNumber"
        const val COL_PASSWORD = "Password"

        // Table Concerts
        const val TABLE_CONCERTS = "Concerts"
        const val COL_CONCERT_ID = "ConcertID"
        const val COL_TITLE = "Title"
        const val COL_ARTIST = "Artist"
        const val COL_VENUE = "Venue"
        const val COL_CITY = "City"
        const val COL_LATITUDE = "Latitude"
        const val COL_LONGITUDE = "Longitude"
        const val COL_DATE = "Date"
        const val COL_PRICE = "Price"
        const val COL_IMAGE = "Image"
        const val COL_DESCRIPTION = "Description"
        const val COL_STOCK = "Stock"

        // Table Transaction (quoted in SQL queries because Transaction is a reserved SQL keyword)
        const val TABLE_TRANSACTION = "Transaction"
        const val COL_TRANSACTION_ID = "TransactionID"
        const val COL_TX_USER_ID = "UserId"
        const val COL_TX_CONCERT_ID = "ConcertId"
        const val COL_QUANTITY = "Quantity"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EMAIL VARCHAR(255) NOT NULL,
                $COL_USERNAME VARCHAR(255) NOT NULL UNIQUE,
                $COL_PHONE VARCHAR(255) NOT NULL,
                $COL_PASSWORD VARCHAR(255) NOT NULL
            )
        """.trimIndent()

        val createConcertsTable = """
            CREATE TABLE $TABLE_CONCERTS (
                $COL_CONCERT_ID INTEGER PRIMARY KEY,
                $COL_TITLE VARCHAR(255) NOT NULL,
                $COL_ARTIST VARCHAR(255) NOT NULL,
                $COL_VENUE VARCHAR(255) NOT NULL,
                $COL_CITY VARCHAR(255) NOT NULL,
                $COL_LATITUDE REAL NOT NULL,
                $COL_LONGITUDE REAL NOT NULL,
                $COL_DATE VARCHAR(255) NOT NULL,
                $COL_PRICE INTEGER NOT NULL,
                $COL_IMAGE VARCHAR(255) NOT NULL,
                $COL_DESCRIPTION VARCHAR(255) NOT NULL,
                $COL_STOCK INTEGER NOT NULL
            )
        """.trimIndent()

        val createTransactionTable = """
            CREATE TABLE "$TABLE_TRANSACTION" (
                $COL_TRANSACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TX_USER_ID INTEGER NOT NULL,
                $COL_TX_CONCERT_ID INTEGER NOT NULL,
                $COL_QUANTITY INTEGER NOT NULL,
                FOREIGN KEY ($COL_TX_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COL_TX_CONCERT_ID) REFERENCES $TABLE_CONCERTS($COL_CONCERT_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createConcertsTable)
        db.execSQL(createTransactionTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS \"$TABLE_TRANSACTION\"")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONCERTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // ==================== USER OPERATIONS ====================

    fun isUsernameTaken(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_USER_ID),
            "$COL_USERNAME = ?",
            arrayOf(username),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun registerUser(
        username: String,
        password: String,
        email: String,
        phoneNumber: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD, password)
            put(COL_EMAIL, email)
            put(COL_PHONE, phoneNumber)
        }
        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    fun authenticateUser(username: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COL_USERNAME = ? AND $COL_PASSWORD = ?",
            arrayOf(username, password),
            null,
            null,
            null
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD))
            )
        }
        cursor.close()
        return user
    }

    fun getUserById(userId: Int): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COL_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD))
            )
        }
        cursor.close()
        return user
    }

    // ==================== CONCERT OPERATIONS ====================

    /**
     * Inserts concert fetched from the JSON API if it does not already exist in SQLite,
     * preserving any stock decrements from ticket purchases.
     */
    fun insertConcertIfNotExists(concert: Concert) {
        val db = writableDatabase
        val cursor = db.query(
            TABLE_CONCERTS,
            arrayOf(COL_CONCERT_ID),
            "$COL_CONCERT_ID = ?",
            arrayOf(concert.concertId.toString()),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()

        if (!exists) {
            val values = ContentValues().apply {
                put(COL_CONCERT_ID, concert.concertId)
                put(COL_TITLE, concert.title)
                put(COL_ARTIST, concert.artist)
                put(COL_VENUE, concert.venue)
                put(COL_CITY, concert.city)
                put(COL_LATITUDE, concert.latitude)
                put(COL_LONGITUDE, concert.longitude)
                put(COL_DATE, concert.date)
                put(COL_PRICE, concert.price)
                put(COL_IMAGE, concert.image)
                put(COL_DESCRIPTION, concert.description)
                put(COL_STOCK, concert.stock)
            }
            db.insert(TABLE_CONCERTS, null, values)
        }
    }

    fun getAllConcerts(): List<Concert> {
        val list = mutableListOf<Concert>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CONCERTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_CONCERT_ID ASC"
        )
        while (cursor.moveToNext()) {
            list.add(cursorToConcert(cursor))
        }
        cursor.close()
        return list
    }

    fun getConcertById(concertId: Int): Concert? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CONCERTS,
            null,
            "$COL_CONCERT_ID = ?",
            arrayOf(concertId.toString()),
            null,
            null,
            null
        )
        var concert: Concert? = null
        if (cursor.moveToFirst()) {
            concert = cursorToConcert(cursor)
        }
        cursor.close()
        return concert
    }

    /**
     * Searches locally cached concerts by title, artist, or city (case-insensitive, partial match).
     */
    fun searchConcerts(keyword: String): List<Concert> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            return getAllConcerts()
        }
        val list = mutableListOf<Concert>()
        val db = readableDatabase
        val likePattern = "%$trimmed%"
        val selection =
            "LOWER($COL_TITLE) LIKE LOWER(?) OR LOWER($COL_ARTIST) LIKE LOWER(?) OR LOWER($COL_CITY) LIKE LOWER(?)"
        val selectionArgs = arrayOf(likePattern, likePattern, likePattern)
        val cursor = db.query(
            TABLE_CONCERTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COL_CONCERT_ID ASC"
        )
        while (cursor.moveToNext()) {
            list.add(cursorToConcert(cursor))
        }
        cursor.close()
        return list
    }

    private fun cursorToConcert(cursor: android.database.Cursor): Concert {
        return Concert(
            concertId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CONCERT_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            artist = cursor.getString(cursor.getColumnIndexOrThrow(COL_ARTIST)),
            venue = cursor.getString(cursor.getColumnIndexOrThrow(COL_VENUE)),
            city = cursor.getString(cursor.getColumnIndexOrThrow(COL_CITY)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE)),
            date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
            price = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRICE)),
            image = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
            stock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STOCK))
        )
    }

    // ==================== TRANSACTION OPERATIONS ====================

    /**
     * Inserts a new transaction and decrements the concert's available stock atomically.
     */
    fun buyTicket(userId: Int, concertId: Int, quantity: Int): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val concert = getConcertById(concertId) ?: return false
            if (quantity <= 0 || quantity > concert.stock) {
                return false
            }

            val txValues = ContentValues().apply {
                put(COL_TX_USER_ID, userId)
                put(COL_TX_CONCERT_ID, concertId)
                put(COL_QUANTITY, quantity)
            }
            val insertResult = db.insert("\"$TABLE_TRANSACTION\"", null, txValues)
            if (insertResult == -1L) {
                return false
            }

            val newStock = concert.stock - quantity
            val stockValues = ContentValues().apply {
                put(COL_STOCK, newStock)
            }
            val updatedRows = db.update(
                TABLE_CONCERTS,
                stockValues,
                "$COL_CONCERT_ID = ?",
                arrayOf(concertId.toString())
            )
            if (updatedRows > 0) {
                db.setTransactionSuccessful()
                true
            } else {
                false
            }
        } finally {
            db.endTransaction()
        }
    }

    fun getTransactionsByUser(userId: Int): List<TransactionItem> {
        val list = mutableListOf<TransactionItem>()
        val db = readableDatabase
        val query = """
            SELECT t.$COL_TRANSACTION_ID,
                   t.$COL_TX_USER_ID,
                   t.$COL_TX_CONCERT_ID,
                   t.$COL_QUANTITY,
                   c.$COL_TITLE,
                   c.$COL_VENUE,
                   c.$COL_DATE,
                   c.$COL_IMAGE
            FROM "$TABLE_TRANSACTION" t
            INNER JOIN $TABLE_CONCERTS c ON t.$COL_TX_CONCERT_ID = c.$COL_CONCERT_ID
            WHERE t.$COL_TX_USER_ID = ?
            ORDER BY t.$COL_TRANSACTION_ID DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            list.add(
                TransactionItem(
                    transactionId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TX_USER_ID)),
                    concertId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TX_CONCERT_ID)),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY)),
                    concertTitle = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                    concertVenue = cursor.getString(cursor.getColumnIndexOrThrow(COL_VENUE)),
                    concertDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                    concertImage = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE))
                )
            )
        }
        cursor.close()
        return list
    }

    fun updateTransactionQuantity(transactionId: Int, newQuantity: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_QUANTITY, newQuantity)
        }
        val rows = db.update(
            "\"$TABLE_TRANSACTION\"",
            values,
            "$COL_TRANSACTION_ID = ?",
            arrayOf(transactionId.toString())
        )
        return rows > 0
    }

    fun deleteTransaction(transactionId: Int): Boolean {
        val db = writableDatabase
        val rows = db.delete(
            "\"$TABLE_TRANSACTION\"",
            "$COL_TRANSACTION_ID = ?",
            arrayOf(transactionId.toString())
        )
        return rows > 0
    }
}
