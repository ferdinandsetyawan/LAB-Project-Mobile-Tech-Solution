package com.example.concertx.ui.detail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.concertx.R
import com.example.concertx.database.DatabaseHelper
import com.example.concertx.model.Concert
import com.example.concertx.utils.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.NumberFormat
import java.util.Locale

class ConcertDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_CONCERT_ID = "extra_concert_id"
    }

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var ivBackDetail: ImageView
    private lateinit var ivDetailPoster: ImageView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailArtist: TextView
    private lateinit var tvDetailVenue: TextView
    private lateinit var tvDetailCity: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailPrice: TextView
    private lateinit var tvDetailStock: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var etBuyQuantity: EditText
    private lateinit var btnBuyTicket: Button

    private var concertId: Int = -1
    private var currentConcert: Concert? = null
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_concert_detail)

        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        concertId = intent.getIntExtra(EXTRA_CONCERT_ID, -1)
        if (concertId == -1) {
            finish()
            return
        }

        ivBackDetail = findViewById(R.id.ivBackDetail)
        ivDetailPoster = findViewById(R.id.ivDetailPoster)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailArtist = findViewById(R.id.tvDetailArtist)
        tvDetailVenue = findViewById(R.id.tvDetailVenue)
        tvDetailCity = findViewById(R.id.tvDetailCity)
        tvDetailDate = findViewById(R.id.tvDetailDate)
        tvDetailPrice = findViewById(R.id.tvDetailPrice)
        tvDetailStock = findViewById(R.id.tvDetailStock)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        etBuyQuantity = findViewById(R.id.etBuyQuantity)
        btnBuyTicket = findViewById(R.id.btnBuyTicket)

        ivBackDetail.setOnClickListener {
            finish()
        }

        // Buy button starts disabled and only becomes enabled once a valid quantity is entered
        btnBuyTicket.isEnabled = false
        etBuyQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString()?.trim().orEmpty()
                val qty = input.toIntOrNull()
                btnBuyTicket.isEnabled = (qty != null && qty > 0)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnBuyTicket.setOnClickListener {
            handleBuyTicket()
        }

        loadConcertDetail()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun loadConcertDetail() {
        val concert = dbHelper.getConcertById(concertId)
        if (concert == null) {
            Toast.makeText(this, "Concert not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentConcert = concert

        tvDetailTitle.text = concert.title
        tvDetailArtist.text = "Artist: ${concert.artist}"
        tvDetailVenue.text = "Venue: ${concert.venue}"
        tvDetailCity.text = "City: ${concert.city}"
        tvDetailDate.text = "Date: ${concert.date}"

        val formattedPrice = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(concert.price)
        tvDetailPrice.text = "Price: Rp $formattedPrice"
        tvDetailStock.text = "Available Stock: ${concert.stock}"
        tvDetailDescription.text = concert.description

        val resId = resources.getIdentifier(
            concert.image.lowercase(),
            "drawable",
            packageName
        )
        if (resId != 0) {
            ivDetailPoster.setImageResource(resId)
        } else {
            ivDetailPoster.setImageResource(R.drawable.lany)
        }

        updateMapMarker()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        updateMapMarker()
    }

    private fun updateMapMarker() {
        val map = googleMap ?: return
        val concert = currentConcert ?: return

        val venueLatLng = LatLng(concert.latitude, concert.longitude)
        map.clear()
        val marker = map.addMarker(
            MarkerOptions()
                .position(venueLatLng)
                .title(concert.venue)
        )
        marker?.showInfoWindow()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(venueLatLng, 15f))
    }

    private fun handleBuyTicket() {
        val concert = dbHelper.getConcertById(concertId) ?: return
        currentConcert = concert

        val input = etBuyQuantity.text.toString().trim()

        // 1. Validate quantity must be filled
        if (input.isEmpty()) {
            Toast.makeText(this, "Quantity must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Validate quantity must be a number and more than 0
        val quantity = input.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            Toast.makeText(
                this,
                "Quantity must be a number and more than 0",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 3. Validate quantity must not exceed the concert's available ticket stock
        if (quantity > concert.stock) {
            Toast.makeText(
                this,
                "Quantity exceeds available ticket stock (${concert.stock} left)",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val userId = sessionManager.getLoggedInUserId()
        val success = dbHelper.buyTicket(
            userId = userId,
            concertId = concert.concertId,
            quantity = quantity
        )

        if (success) {
            Toast.makeText(
                this,
                "Successfully purchased $quantity ticket(s) for ${concert.title}!",
                Toast.LENGTH_SHORT
            ).show()
            etBuyQuantity.setText("")
            loadConcertDetail()
        } else {
            Toast.makeText(
                this,
                "Failed to complete purchase. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
