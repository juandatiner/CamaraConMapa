package com.example.camaraconmapa

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "DirectionsRepo"

object DirectionsRepository {

    suspend fun fetchWalkingRoute(
        origin: LatLng,
        destination: LatLng,
        apiKey: String
    ): List<LatLng> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext listOf(origin, destination)
        try {
            val url = URL(
                "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=walking&key=$apiKey"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val status = json.optString("status")
            if (status != "OK") {
                Log.w(TAG, "Directions status=$status body=${body.take(200)}")
                return@withContext listOf(origin, destination)
            }
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext listOf(origin, destination)
            val encoded = routes.getJSONObject(0)
                .getJSONObject("overview_polyline")
                .getString("points")
            PolyUtil.decode(encoded)
        } catch (e: Exception) {
            Log.e(TAG, "fetchWalkingRoute failed", e)
            listOf(origin, destination)
        }
    }
}
