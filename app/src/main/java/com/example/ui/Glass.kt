package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBlobBlue
import com.example.ui.theme.GlassBlobCyan
import com.example.ui.theme.GlassBlobWhite
import com.example.ui.theme.GlassFillBottom
import com.example.ui.theme.GlassFillTop
import com.example.ui.theme.GlassShadow
import com.example.ui.theme.GlassSkyBottom
import com.example.ui.theme.GlassSkyMid
import com.example.ui.theme.GlassSkyTop
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Light blue glass look
//
// Android cannot blur what is behind a panel, so "glass" is built from three layers:
//   1. a soft sky gradient with slowly drifting colour blobs (GlassBackground)
//   2. panels filled with translucent white, so the blobs show through (GlassCard)
//   3. a bright rim around each panel, like light catching the edge of real glass
// ---------------------------------------------------------------------------------------------

/** Full-screen sky gradient with three soft blobs that drift very slowly. */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "glass-background")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glass-drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GlassSkyTop, GlassSkyMid, GlassSkyBottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val cyanCenter = Offset(w * (0.15f + 0.25f * drift), h * (0.10f + 0.06f * drift))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(GlassBlobCyan, Color.Transparent),
                    center = cyanCenter,
                    radius = w * 0.80f
                )
            )

            val blueCenter = Offset(w * (0.95f - 0.30f * drift), h * (0.55f + 0.08f * drift))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(GlassBlobBlue, Color.Transparent),
                    center = blueCenter,
                    radius = w * 0.90f
                )
            )

            val whiteCenter = Offset(w * (0.25f + 0.20f * drift), h * (0.95f - 0.10f * drift))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(GlassBlobWhite, Color.Transparent),
                    center = whiteCenter,
                    radius = w * 0.85f
                )
            )
        }
        content()
    }
}

/**
 * Frosted glass panel. Drop-in replacement for Material's Card: same modifier / shape / content,
 * but translucent with a bright rim and a soft blue shadow.
 *
 * @param borderBrush optional accent rim (for example a blue to cyan gradient) instead of the
 *        default white one.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 14.dp,
            shape = shape,
            ambientColor = GlassShadow,
            spotColor = GlassShadow
        ),
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(GlassFillTop, GlassFillBottom)), shape)
                .border(1.dp, borderBrush ?: glassRim, shape),
            content = content
        )
    }
}

private val glassRim: Brush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.95f),
        Color.White.copy(alpha = 0.30f),
        Color.White.copy(alpha = 0.70f)
    )
)

// ---------------------------------------------------------------------------------------------
// Buttons. Same parameters as Material's Button / OutlinedButton (so call sites barely change),
// plus an iOS-style spring squish when pressed.
// ---------------------------------------------------------------------------------------------

@Composable
fun IosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .pressScale(interactionSource)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = GlassShadow,
                        spotColor = GlassShadow
                    )
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun IosOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.45f),
            contentColor = ElectricBlue
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

// ---------------------------------------------------------------------------------------------
// iOS-style motion
// ---------------------------------------------------------------------------------------------

/** Shrinks slightly while pressed and springs back with a small bounce. */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "press-scale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Tap target without the grey Material ripple; the spring squish is the feedback instead. */
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this
        .pressScale(interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Fades in while rising from slightly below. Give each item in a list a higher [index] so they
 * arrive one after another.
 */
fun Modifier.staggeredAppear(index: Int, stepMillis: Int = 70): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMillis.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
        )
    }
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 36.dp.toPx()
    }
}

/** Slow up-and-down drift, like a floating iOS widget. */
fun Modifier.gentleFloat(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "gentle-float")
    val offset by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentle-float-offset"
    )
    graphicsLayer {
        translationY = offset.dp.toPx()
    }
}
