package com.example.concertx.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.concertx.R
import com.example.concertx.database.DatabaseHelper
import com.example.concertx.model.Concert
import com.example.concertx.ui.adapter.ConcertAdapter
import com.example.concertx.ui.detail.ConcertDetailActivity
import com.example.concertx.ui.search.SearchActivity

class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvConcerts: RecyclerView
    private lateinit var ivSearch: ImageView
    private lateinit var concertAdapter: ConcertAdapter

    companion object {
        private const val CONCERT_API_URL =
            "https://gist.githubusercontent.com/axelkrnwn/31139c9cfee4aca4acee3a36907a8c7e/raw/f4be1d6783d4331363e92b1bc450bf77259ea07c/concertx.json"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        rvConcerts = view.findViewById(R.id.rvConcerts)
        ivSearch = view.findViewById(R.id.ivSearch)

        concertAdapter = ConcertAdapter(emptyList()) { selectedConcert ->
            val intent = Intent(requireContext(), ConcertDetailActivity::class.java)
            intent.putExtra(ConcertDetailActivity.EXTRA_CONCERT_ID, selectedConcert.concertId)
            startActivity(intent)
        }

        rvConcerts.layoutManager = LinearLayoutManager(requireContext())
        rvConcerts.adapter = concertAdapter

        ivSearch.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        // Show any already cached concerts immediately, then fetch from JSON API using Volley
        loadConcertsFromDatabase()
        fetchConcertsFromApi()
    }

    override fun onResume() {
        super.onResume()
        loadConcertsFromDatabase()
    }

    private fun fetchConcertsFromApi() {
        val context = context ?: return
        val requestQueue = Volley.newRequestQueue(context)

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET,
            CONCERT_API_URL,
            null,
            { response ->
                try {
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        val concert = Concert(
                            concertId = obj.getInt("concertId"),
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            venue = obj.getString("venue"),
                            city = obj.getString("city"),
                            latitude = obj.getDouble("latitude"),
                            longitude = obj.getDouble("longitude"),
                            date = obj.getString("date"),
                            price = obj.getInt("price"),
                            image = obj.getString("image"),
                            description = obj.getString("description"),
                            stock = obj.getInt("stock")
                        )
                        dbHelper.insertConcertIfNotExists(concert)
                    }
                    if (isAdded) {
                        loadConcertsFromDatabase()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Error parsing concert data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            { error ->
                error.printStackTrace()
                if (isAdded && dbHelper.getAllConcerts().isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch concerts from API",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        requestQueue.add(jsonArrayRequest)
    }

    private fun loadConcertsFromDatabase() {
        val concerts = dbHelper.getAllConcerts()
        concertAdapter.updateData(concerts)
    }
}
