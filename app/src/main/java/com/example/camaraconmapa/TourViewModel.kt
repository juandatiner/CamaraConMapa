package com.example.camaraconmapa

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

data class PhotoEntry(
    val name: String,
    val uri: Uri,
    val location: LatLng,
    val aspectRatio: Float = 3f / 4f
)

class TourViewModel : ViewModel() {
    var isRecording by mutableStateOf(false)
        private set

    val photos: MutableList<PhotoEntry> = mutableStateListOf()
    val path: MutableList<LatLng> = mutableStateListOf()

    fun toggleRecording() {
        if (isRecording) {
            photos.clear()
            path.clear()
        }
        isRecording = !isRecording
    }

    fun addPhoto(entry: PhotoEntry, apiKey: String) {
        val previous = photos.lastOrNull()?.location
        photos.add(entry)
        if (previous == null) {
            path.add(entry.location)
        } else {
            viewModelScope.launch {
                val segment = DirectionsRepository.fetchWalkingRoute(
                    origin = previous,
                    destination = entry.location,
                    apiKey = apiKey
                )
                // drop first point to avoid duplicating previous endpoint
                val toAdd = if (segment.isNotEmpty()) segment.drop(1) else listOf(entry.location)
                path.addAll(toAdd)
            }
        }
    }
}
