package com.example.subscription_manager.ui.screens.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val LOADING_DURATION_MS = 5_000L
private const val FINAL_HOLD_DURATION_MS = 5_000L

private const val LOADING_MESSAGE = "Wait, Serhiy is calculating.."
private const val FINAL_MESSAGE = "CALCULATION COMPLETE\nAnd the result is: You’re the best,\nVanessa Baron. ❤️"

private val SplashBackground = Color(0xFF15191D)
private val HeartRedTop = Color(0xFFFF4B5F)
private val HeartRedMiddle = Color(0xFFD7193E)
private val HeartRedBottom = Color(0xFFA9061F)
private val HeartOutline = Color(0xFFC9CED3)
private val HeartShadow = Color(0x55000000)
private val TextWhite = Color(0xFFF8F8F8)
private val SubtleText = Color(0xFFE8E8E8)
private val SparkleColor = Color(0xFF6B7280)

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasFinished = remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var message by remember { mutableStateOf(LOADING_MESSAGE) }

    LaunchedEffect(Unit) {
        val fillStartNanos = System.nanoTime()
        val loadingDurationNanos = LOADING_DURATION_MS * 1_000_000L

        while (true) {
            val elapsedSinceFillStart = System.nanoTime() - fillStartNanos
            progress = (elapsedSinceFillStart.toFloat() / loadingDurationNanos.toFloat()).coerceIn(0f, 1f)
            if (progress >= 1f) break
            delay(16)
        }

        if (!hasFinished.value) {
            message = FINAL_MESSAGE
        }

        delay(FINAL_HOLD_DURATION_MS)
        if (!hasFinished.value) {
            hasFinished.value = true
            onFinished()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground)
            .pointerInput(onFinished) {
                detectTapGestures {
                    if (!hasFinished.value) {
                        hasFinished.value = true
                        onFinished()
                    }
                }
            }
    ) {
        val heartSize = 180.dp
        val heartCenterY = maxHeight * 0.34f
        val resultTextTop = heartCenterY + heartSize / 2f + 36.dp

        HeartWithPercentage(
            progress = progress,
            isComplete = message == FINAL_MESSAGE,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = heartCenterY - heartSize / 2f)
                .size(heartSize)
        )

        ResultText(
            message = message,
            isComplete = message == FINAL_MESSAGE,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = resultTextTop)
                .padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun HeartWithPercentage(
    progress: Float,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    val percentage = if (isComplete) 100 else (progress * 100).toInt()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawReferenceHeart(progress = progress)
        }

        Text(
            text = "$percentage%",
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontSize = 44.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResultText(
    message: String,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isComplete) {
            Text(
                text = "CALCULATION COMPLETE",
                color = TextWhite,
                fontFamily = FontFamily.Serif,
                fontSize = 21.sp,
                lineHeight = 28.sp,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = buildAnnotatedString {
                    append("And the result is: You’re the best,\n")
                    append("Vanessa Baron. ")
                    withStyle(SpanStyle(color = HeartRedMiddle)) {
                        append("❤️")
                    }
                },
                color = SubtleText,
                fontFamily = FontFamily.Serif,
                fontSize = 19.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = message,
                color = TextWhite,
                fontFamily = FontFamily.Serif,
                fontSize = 19.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun DrawScope.drawReferenceHeart(
    progress: Float
) {
    val unitWidth = 100f
    val unitHeight = 86f
    val scale = size.minDimension * 0.94f / unitWidth
    val offsetX = (size.width - unitWidth * scale) / 2f
    val offsetY = (size.height - unitHeight * scale) / 2f
    val normalizedProgress = progress.coerceIn(0f, 1f)
    val emptyHeartColor = Color(0xFF20262C)

    val shadowPath = createHeartPath(offsetX, offsetY + 7f * scale, scale)
    val filledHeartPath = createHeartPath(offsetX, offsetY, scale)

    drawPath(
        path = shadowPath,
        color = HeartShadow
    )

    drawPath(
        path = filledHeartPath,
        color = emptyHeartColor
    )

    if (normalizedProgress > 0f) {
        val fillHeight = unitHeight * scale * normalizedProgress
        clipPath(filledHeartPath) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(HeartRedTop, HeartRedMiddle, HeartRedBottom),
                    start = Offset(offsetX, offsetY),
                    end = Offset(offsetX + unitWidth * scale, offsetY + unitHeight * scale)
                ),
                topLeft = Offset(offsetX, offsetY + unitHeight * scale - fillHeight),
                size = Size(width = unitWidth * scale, height = fillHeight)
            )
        }
    }

    drawPath(
        path = filledHeartPath,
        color = HeartOutline,
        style = Stroke(width = 4f * scale)
    )
}

private fun createHeartPath(offsetX: Float, offsetY: Float, scale: Float): Path {
    val unitHeight = 86f

    fun x(value: Float): Float = offsetX + value * scale
    fun y(value: Float): Float = offsetY + value * scale

    return Path().apply {
        moveTo(x(50f), y(unitHeight))
        cubicTo(x(24f), y(62f), x(0f), y(42f), x(0f), y(22f))
        cubicTo(x(0f), y(8f), x(11f), y(0f), x(24f), y(0f))
        cubicTo(x(34f), y(0f), x(43f), y(6f), x(50f), y(16f))
        cubicTo(x(57f), y(6f), x(66f), y(0f), x(76f), y(0f))
        cubicTo(x(89f), y(0f), x(100f), y(8f), x(100f), y(22f))
        cubicTo(x(100f), y(42f), x(76f), y(62f), x(50f), y(unitHeight))
        close()
    }
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
