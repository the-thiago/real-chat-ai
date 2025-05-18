package com.thiago.realchat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated "ball of yarn" built from curved lines. The lines rotate around the
 * center and their curvature subtly pulses, giving a loose–thread look. When
 * [isRecording] is true the ball spins faster and pulses more aggressively.
 */
@Composable
fun YarnBallVisualizer(
    isRecording: Boolean,
    amplitude: Float = 0f,
    modifier: Modifier = Modifier
) {
    // Simple infinite rotation based on time
    val infiniteTransition = rememberInfiniteTransition(label = "yarnBallRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 2000 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val baseStroke = if (isRecording) 6f else 4f
    // Dynamic hue cycling for a more vivid look
    val hueBase = ((rotation + amplitude * 360f) * 0.4f + if (isRecording) 160f else 220f) % 360f
    val baseColor = Color.hsv(hueBase, 0.7f, 1f)

    // Pulse scale controls ball breathing
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Additional small oscillation that bends the threads while rotating
    val curvaturePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 1600 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "curvaturePhase"
    )

    // Apply amplitude to rotation offset for faster spin
    val rotationOffset = amplitude * 360f

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = this.center
        val baseRadius = size.minDimension * 0.25f
        val radius = baseRadius * (if (isRecording) pulse else 1f) * (1f + amplitude * 0.3f)

        // Draw subtle radial gradient behind for 3D shading
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor.copy(alpha = 0.15f), baseColor.copy(alpha = 0.05f), Color.Transparent),
                center = center,
                radius = radius * 1.25f
            ),
            radius = radius * 1.25f,
            center = center
        )

        val lines = 6 // more threads for a fuller ball
        for (i in 0 until lines) {
            val angleDeg = rotation + rotationOffset + i * 360f / lines
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

            // Direction unit vector
            val dx = cos(angleRad)
            val dy = sin(angleRad)

            val start = Offset(center.x - dx * radius, center.y - dy * radius)
            val end = Offset(center.x + dx * radius, center.y + dy * radius)

            // Perpendicular vector for control point
            val perp = Offset(-dy, dx)
            val curvatureMag = radius * 0.35f * (0.6f + 0.4f * sin(curvaturePhase + i) + amplitude * 0.5f)
            val control = Offset(center.x + perp.x * curvatureMag, center.y + perp.y * curvatureMag)

            val depth = (cos(angleRad) + 1f) / 2f // 0 (back) .. 1 (front)

            val strokeWidth = baseStroke * (0.6f + 0.8f * depth)
            val color = baseColor.copy(alpha = 0.3f + 0.7f * depth)

            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticBezierTo(control.x, control.y, end.x, end.y)
            }

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0f), color, color.copy(alpha = 0f)),
                    start = start,
                    end = end
                ),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/** Horizontal waveform visualizer that stretches across the screen. */
@Composable
fun WaveformVisualizer(
    amplitude: Float, // 0f..1f
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00C853),
) {
    // Animate phase shift for traveling wave and subtle amplitude breathing
    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )

    val breath by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breath"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerY = size.height / 2f
        val maxAmp = size.height / 2.5f // max peak
        val path = Path()
        val points = 120
        for (i in 0..points) {
            val x = size.width * i / points.toFloat()
            val progress = i / points.toFloat()
            // Composite waveform: fundamental + 2nd harmonic for richer look
            val fundamental = sin((progress * 2f * PI + phase).toFloat())
            val harmonic = 0.5f * sin((progress * 4f * PI + phase * 1.3f).toFloat())
            val env = (1f - (progress - 0.5f).let { it * it * 4f }.coerceIn(0f, 1f)) // subtle tapering near edges

            val y = centerY + (fundamental + harmonic) * maxAmp * amplitude * breath * env
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(color.copy(alpha = 0.1f), color, color.copy(alpha = 0.1f))
            ),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
} 