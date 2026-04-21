package com.example.camaraconmapa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val V_WIDTH = 75.dp
private val V_HEIGHT = 100.dp
private val H_WIDTH = 110.dp
private val H_HEIGHT = 49.dp
private val BAR_HEIGHT = 110.dp

@Composable
fun PhotoBar(
    viewModel: TourViewModel,
    modifier: Modifier = Modifier
) {
    val count = viewModel.photos.size

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
        color = Color.White
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (count == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Toma tu primera foto del recorrido",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5C5C5C)
                    )
                }
            } else {
                LazyHorizontalStaggeredGrid(
                    rows = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    horizontalItemSpacing = 2.dp,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = viewModel.photos,
                        span = { photo ->
                            if (photo.aspectRatio > 1f) StaggeredGridItemSpan.SingleLane
                            else StaggeredGridItemSpan.FullLine
                        }
                    ) { photo ->
                        val landscape = photo.aspectRatio > 1f
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(
                                    width = if (landscape) H_WIDTH else V_WIDTH,
                                    height = if (landscape) H_HEIGHT else V_HEIGHT
                                )
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}
