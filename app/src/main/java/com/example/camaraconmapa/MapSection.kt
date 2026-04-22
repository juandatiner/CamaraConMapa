package com.example.camaraconmapa

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val FALLBACK_LOCATION = LatLng(4.7110, -74.0721) // Bogotá

@OptIn(MapsComposeExperimentalApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MapSection(
    viewModel: TourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()
    var centered by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(FALLBACK_LOCATION, 15f)
    }

    val hasLocationPerm = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    suspend fun smoothCenterOn(loc: Location, targetZoom: Float = 17f) {
        val latLng = LatLng(loc.latitude, loc.longitude)
        val current = cameraPositionState.position.target
        val distOut = FloatArray(1)
        Location.distanceBetween(
            current.latitude, current.longitude,
            latLng.latitude, latLng.longitude,
            distOut
        )
        val distanceMeters = distOut[0]

        if (!centered) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, targetZoom),
                durationMs = 800
            )
        } else if (distanceMeters > 3000f) {
            val overview = minOf(cameraPositionState.position.zoom - 3f, 11f)
            cameraPositionState.animate(
                CameraUpdateFactory.zoomTo(overview),
                durationMs = 350
            )
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(latLng),
                durationMs = 650
            )
            cameraPositionState.animate(
                CameraUpdateFactory.zoomTo(targetZoom),
                durationMs = 450
            )
        } else {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, targetZoom),
                durationMs = 900
            )
        }
        centered = true
    }

    fun centerOn(loc: Location) {
        scope.launch { smoothCenterOn(loc) }
    }

    LaunchedEffect(hasLocationPerm) {
        if (!hasLocationPerm) return@LaunchedEffect
        fused.lastLocation.addOnSuccessListener { last ->
            if (last != null && !centered) centerOn(last)
        }
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { cur ->
                if (cur != null) centerOn(cur)
            }
    }


    val clusters = remember(viewModel.photos.size, viewModel.photos.toList()) {
        viewModel.photos.groupBy { photo ->
            val lat = (photo.location.latitude * 10000).toInt()
            val lng = (photo.location.longitude * 10000).toInt()
            lat to lng
        }.values.toList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPerm),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = true,
                compassEnabled = true
            )
        ) {
            if (viewModel.path.size >= 2) {
                Polyline(
                    points = viewModel.path.toList(),
                    color = Color(0xFF1976D2),
                    width = 12f
                )
            }
            clusters.forEach { group ->
                val representative = group.last()
                PhotoMarkerNode(photo = representative, count = group.size)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            val recording = viewModel.isRecording
            val count = viewModel.photos.size

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(18.dp))
                FilledTonalIconButton(
                    onClick = { showGallery = true },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.92f)
                    )
                ) {
                    BadgedBox(
                        badge = {
                            if (count > 0) {
                                Badge { Text("$count") }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Ver fotos",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(18.dp))
                FilledIconButton(
                    onClick = { viewModel.toggleRecording() },
                    modifier = Modifier.size(48.dp),
                    shape = if (recording) RoundedCornerShape(6.dp) else CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (recording) Color(0xFFB00020)
                        else Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        imageVector = if (recording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (recording) "Pausar recorrido" else "Iniciar recorrido",
                        tint = Color.White
                    )
                }
            }
        }

        FilledTonalIconButton(
            onClick = {
                if (!hasLocationPerm) return@FilledTonalIconButton
                fused.lastLocation.addOnSuccessListener { last ->
                    if (last != null) centerOn(last)
                }
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { cur ->
                        if (cur != null) centerOn(cur)
                    }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .size(48.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.White
            )
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "Centrar en mi ubicación",
                tint = Color(0xFF1976D2)
            )
        }
    }

    if (showGallery) {
        PhotoGalleryDialog(
            photos = viewModel.photos,
            onDismiss = { showGallery = false }
        )
    }
}


@OptIn(MapsComposeExperimentalApi::class)
@com.google.maps.android.compose.GoogleMapComposable
@Composable
private fun PhotoMarkerNode(photo: PhotoEntry, count: Int) {
    val context = LocalContext.current
    var bitmap by remember(photo.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photo.uri) {
        val request = ImageRequest.Builder(context)
            .data(photo.uri)
            .size(160, 160)
            .allowHardware(false)
            .build()
        val result = ImageLoader(context).execute(request)
        if (result is SuccessResult) {
            val drawable = result.drawable
            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
            }
        }
    }

    MarkerComposable(
        (bitmap ?: photo.uri.toString()),
        count,
        photo.aspectRatio,
        state = MarkerState(position = photo.location),
        title = if (count > 1) "${photo.name} (+${count - 1})" else photo.name
    ) {
        StackedPhotoMarker(bitmap = bitmap, count = count, aspectRatio = photo.aspectRatio)
    }
}

@Composable
private fun StackedPhotoMarker(bitmap: Bitmap?, count: Int, aspectRatio: Float) {
    val maxDim = 72f
    val (w, h) = if (aspectRatio > 1f) {
        maxDim.dp to (maxDim / aspectRatio).dp
    } else {
        (maxDim * aspectRatio).dp to maxDim.dp
    }
    Box(modifier = Modifier.width(w + 16.dp).height(h + 10.dp)) {
        if (count > 1) {
            Box(
                modifier = Modifier
                    .width(w)
                    .height(h)
                    .align(Alignment.Center)
                    .padding(top = 6.dp, start = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, Color(0xFF90CAF9), RoundedCornerShape(12.dp))
            )
            if (count > 2) {
                Box(
                    modifier = Modifier
                        .width(w)
                        .height(h)
                        .align(Alignment.Center)
                        .padding(top = 3.dp, start = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(2.dp, Color(0xFFBBDEFB), RoundedCornerShape(12.dp))
                )
            }
        }

        Box(
            modifier = Modifier
                .width(w)
                .height(h)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(3.dp, Color.White, RoundedCornerShape(12.dp))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, Color(0xFF1976D2), RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE0E0E0))
                        .border(2.dp, Color(0xFF1976D2), RoundedCornerShape(10.dp))
                )
            }
        }

        if (count > 1) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFB00020))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
