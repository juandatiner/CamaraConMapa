package com.example.camaraconmapa

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "CameraSection"
private const val FOLDER = "CamaraConMapa"

@SuppressLint("MissingPermission")
@Composable
fun CameraSection(
    viewModel: TourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .build()
    }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(lensFacing) {
        bindCamera(context, lifecycleOwner, previewView, imageCapture, lensFacing)
    }

    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture.targetRotation = rotation
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = {
                    lensFacing =
                        if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else CameraSelector.LENS_FACING_BACK
                },
                containerColor = Color.White.copy(alpha = 0.85f)
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Cambiar cámara")
            }
            FloatingActionButton(
                onClick = {
                    if (!viewModel.isRecording) {
                        Toast.makeText(
                            context,
                            "Inicia el recorrido primero",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@FloatingActionButton
                    }
                    takePhoto(
                        context = context,
                        imageCapture = imageCapture,
                        executor = mainExecutor,
                        fused = fused,
                        isLandscape = isLandscape,
                        onSaved = { entry ->
                            viewModel.addPhoto(entry, BuildConfig.MAPS_API_KEY)
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Tomar foto")
            }
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

private suspend fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lensFacing: Int
) {
    val provider = awaitCameraProvider(context)
    val preview = Preview.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        )
        .build()
        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
    val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    try {
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
    } catch (e: Exception) {
        Log.e(TAG, "bindCamera failed", e)
    }
}

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider =
    suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }

@SuppressLint("MissingPermission")
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    fused: com.google.android.gms.location.FusedLocationProviderClient,
    isLandscape: Boolean,
    onSaved: (PhotoEntry) -> Unit
) {
    val hasLoc = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    if (!hasLoc) {
        Toast.makeText(context, "Sin permiso de ubicación", Toast.LENGTH_SHORT).show()
        return
    }

    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { loc ->
            if (loc == null) {
                Toast.makeText(context, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val name = "CCM_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$FOLDER")
                }
            }
            val output = ImageCapture.OutputFileOptions
                .Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                .build()
            imageCapture.takePicture(
                output,
                executor,

                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }

                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        val uri: Uri = result.savedUri ?: return
                        val aspect = readImageAspect(context, uri)
                            ?: if (isLandscape) 4f / 3f else 3f / 4f
                        onSaved(
                            PhotoEntry(
                                name = name,
                                uri = uri,
                                location = LatLng(loc.latitude, loc.longitude),
                                aspectRatio = aspect
                            )
                        )
                    }
                }
            )
        }
}

private fun readImageAspect(context: Context, uri: Uri): Float? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    }
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

    val orientation = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    val swap = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE

    val w = if (swap) opts.outHeight else opts.outWidth
    val h = if (swap) opts.outWidth else opts.outHeight
    return w.toFloat() / h.toFloat()
}
