package com.example.subscription_manager.ui.screens.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectTapGestures

private const val LOADING_DURATION_MS = 5_000L
private const val FINAL_HOLD_DURATION_MS = 5_000L
private const val TEXT_TRANSITION_IN_MS = 300
private const val TEXT_TRANSITION_OUT_MS = 200

private const val LOADING_MESSAGE = "Wait, Serhiy is calculating.."
private const val FINAL_MESSAGE = "Calculation complete! And the result is: You're the best, Vanessa Baron. ❤️"

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasFinished by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf(LOADING_MESSAGE) }
    var progress by remember { mutableFloatStateOf(0f) }

    val heartScale by animateFloatAsState(
        targetValue = if (isComplete) 1.08f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "SplashHeartScale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(16)

        val fillStartNanos = System.nanoTime()
        val loadingDurationNanos = LOADING_DURATION_MS * 1_000_000L

        while (true) {
            val elapsedSinceFillStart = System.nanoTime() - fillStartNanos
            progress = (elapsedSinceFillStart.toFloat() / loadingDurationNanos.toFloat()).coerceIn(0f, 1f)
            if (progress >= 1f) break
            kotlinx.coroutines.delay(16)
        }

        if (!hasFinished) {
            isComplete = true
            message = FINAL_MESSAGE
        }

        kotlinx.coroutines.delay(TEXT_TRANSITION_IN_MS + FINAL_HOLD_DURATION_MS)
        if (!hasFinished) {
            hasFinished = true
            onFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(onFinished) {
                detectTapGestures {
                    if (!hasFinished) {
                        hasFinished = true
                        onFinished()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeartProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .size(168.dp)
                    .scale(heartScale)
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = message,
                label = "SplashMessage",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(TEXT_TRANSITION_IN_MS)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(TEXT_TRANSITION_IN_MS)
                        )) togetherWith
                        (fadeOut(animationSpec = tween(TEXT_TRANSITION_OUT_MS)) +
                            scaleOut(
                                targetScale = 1.08f,
                                animationSpec = tween(TEXT_TRANSITION_OUT_MS)
                            ))
                }
            ) { currentMessage ->
                Text(
                    text = currentMessage,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .zIndex(1f)
                )
            }
        }
    }
}

@Composable
private fun HeartProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val fillRed = Color(0xFFE11D48)
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val emptyHeartColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    val strokeWidth = 5.dp
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val normalizedProgress = progress.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val heartGeometry = createHeartGeometry(sizePx)

        drawPath(
            path = heartGeometry.path,
            color = emptyHeartColor,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        val fillHeight = (heartGeometry.bottom - heartGeometry.top) * normalizedProgress
        clipPath(heartGeometry.path) {
            drawRect(
                color = fillRed,
                topLeft = Offset(x = 0f, y = heartGeometry.bottom - fillHeight),
                size = Size(width = sizePx, height = fillHeight)
            )
        }

        drawPath(
            path = heartGeometry.path,
            color = outlineColor,
            style = Stroke(width = strokeWidthPx)
        )
    }
}

private data class HeartGeometry(
    val path: Path,
    val top: Float,
    val bottom: Float
)

private fun createHeartGeometry(size: Float): HeartGeometry {
    val inset = size * 0.08f
    val width = size - inset * 2

    fun x(ratio: Float): Float = inset + width * ratio
    fun y(ratio: Float): Float = inset + width * ratio

    val path = Path().apply {
        fillType = PathFillType.NonZero
        moveTo(x(0.50f), y(0.86f))
        cubicTo(
            x(0.50f),
            y(0.86f),
            x(0.10f),
            y(0.62f),
            x(0.10f),
            y(0.38f)
        )
        cubicTo(
            x(0.10f),
            y(0.20f),
            x(0.24f),
            y(0.08f),
            x(0.40f),
            y(0.08f)
        )
        cubicTo(
            x(0.48f),
            y(0.08f),
            x(0.54f),
            y(0.13f),
            x(0.58f),
            y(0.20f)
        )
        cubicTo(
            x(0.62f),
            y(0.13f),
            x(0.68f),
            y(0.08f),
            x(0.76f),
            y(0.08f)
        )
        cubicTo(
            x(0.92f),
            y(0.08f),
            x(1.00f),
            y(0.20f),
            x(1.00f),
            y(0.38f)
        )
        cubicTo(
            x(1.00f),
            y(0.62f),
            x(0.50f),
            y(0.86f),
            x(0.50f),
            y(0.86f)
        )
        close()
    }

    return HeartGeometry(path = path, top = y(0.08f), bottom = y(0.86f))
}

internal object ColdStartSplashController {
    private var hasConsumedColdStart = false

    fun shouldShowSplash(): Boolean {
        if (hasConsumedColdStart) return false
        hasConsumedColdStart = true
        return true
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(onFinished = {})
}
