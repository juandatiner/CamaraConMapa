package com.example.camaraconmapa

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camaraconmapa.ui.theme.CamaraConMapaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamaraConMapaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF2F2F2)
                ) { innerPadding ->
                    PermissionsGate {
                        MainScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val viewModel: TourViewModel = viewModel()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeLayout(viewModel = viewModel, modifier = modifier)
    } else {
        PortraitLayout(viewModel = viewModel, modifier = modifier)
    }
}

@Composable
private fun PortraitLayout(viewModel: TourViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RoundedSection(modifier = Modifier.fillMaxWidth().weight(1f), bg = Color.Black) {
            CameraSection(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(visible = viewModel.isRecording) {
            PhotoBar(viewModel = viewModel, modifier = Modifier.fillMaxWidth())
        }

        RoundedSection(modifier = Modifier.fillMaxWidth().weight(1f)) {
            MapSection(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun LandscapeLayout(viewModel: TourViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RoundedSection(modifier = Modifier.fillMaxHeight().weight(1f)) {
            MapSection(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier.fillMaxHeight().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundedSection(modifier = Modifier.fillMaxWidth().weight(1f), bg = Color.Black) {
                CameraSection(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
            AnimatedVisibility(visible = viewModel.isRecording) {
                PhotoBar(viewModel = viewModel, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RoundedSection(
    modifier: Modifier = Modifier,
    bg: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
        color = bg
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        ) { content() }
    }
}
