package com.example.concertx.ui.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.concertx.R
import com.example.concertx.database.DatabaseHelper
import com.example.concertx.ui.adapter.ConcertAdapter
import com.example.concertx.ui.detail.ConcertDetailActivity

class SearchActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var ivBackSearch: ImageView
    private lateinit var etSearchKeyword: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvNoConcertsFound: TextView
    private lateinit var concertAdapter: ConcertAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        dbHelper = DatabaseHelper(this)

        ivBackSearch = findViewById(R.id.ivBackSearch)
        etSearchKeyword = findViewById(R.id.etSearchKeyword)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        tvNoConcertsFound = findViewById(R.id.tvNoConcertsFound)

        ivBackSearch.setOnClickListener {
            finish()
        }

        concertAdapter = ConcertAdapter(emptyList()) { selectedConcert ->
            val intent = Intent(this, ConcertDetailActivity::class.java)
            intent.putExtra(ConcertDetailActivity.EXTRA_CONCERT_ID, selectedConcert.concertId)
            startActivity(intent)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = concertAdapter

        // Initial load of all locally cached concerts
        filterConcerts("")

        etSearchKeyword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterConcerts(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        filterConcerts(etSearchKeyword.text.toString())
    }

    private fun filterConcerts(keyword: String) {
        val results = dbHelper.searchConcerts(keyword)
        if (results.isEmpty()) {
            rvSearchResults.visibility = View.GONE
            tvNoConcertsFound.visibility = View.VISIBLE
        } else {
            rvSearchResults.visibility = View.VISIBLE
            tvNoConcertsFound.visibility = View.GONE
            concertAdapter.updateData(results)
        }
    }
}
