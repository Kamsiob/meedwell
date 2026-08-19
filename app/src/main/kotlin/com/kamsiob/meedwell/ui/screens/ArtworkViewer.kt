package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.components.Cover
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screen 13 in the visual reference: the artwork viewer.
 *
 * One tap behind every cover in the app. **Themeless by design**: the complete
 * art on near-black in light mode and dark alike, no text ever on it, pinch to
 * zoom, tap to close.
 *
 * The near-black is deliberately not the theme background. This is the one
 * screen where the artwork is the entire point, and a paper background in light
 * theme would be competing with it. Both themes get the same presentation, and
 * a test asserts that rather than trusting it.
 */
@Composable
fun ArtworkViewer(
    artworkUri: String?,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Themeless. The same near-black in both themes, by construction.
            .background(ARTWORK_VIEWER_GROUND)
            .clickable(role = Role.Button, onClick = onClose)
            .semantics { contentDescription = "Artwork for $title. Tap to close." },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Cover(
                url = artworkUri,
                title = title,
                cornerRadius = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    // **Double tap, one hand.** Pinch needs two, and a double
                    // tap whose second beat was a hair late used to close the
                    // viewer instead of zooming it. Supplying onDoubleTap makes
                    // the single tap wait out the window, which fixes the
                    // misfire as a side effect.
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            },
                            onTap = { onClose() },
                        )
                    },
            )

            // Text sits below the art, on the ground, never over it.
            Text(
                text = title,
                style = MeedwellTheme.typography.rowTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp),
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MeedwellTheme.typography.meta,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 3.dp, start = 24.dp, end = 24.dp),
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onClose)
                .semantics { contentDescription = "Close" },
            contentAlignment = Alignment.Center,
        ) {
            MeedwellIcon(
                MeedwellIcons.Close,
                size = 18.dp,
                tint = Color.White.copy(alpha = 0.85f),
            )
        }

        Text(
            text = "Pinch or double tap to zoom · tap to close",
            style = MeedwellTheme.typography.section,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
        )
    }
}

/**
 * The viewer's ground, identical in both themes.
 *
 * Near-black rather than pure black, per the standing rule that neither theme
 * uses pure black or pure white anywhere.
 */
val ARTWORK_VIEWER_GROUND = Color(0xFF070709)
