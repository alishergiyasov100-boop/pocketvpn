package com.musornibak.pocketvpn.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.musornibak.pocketvpn.data.VpnState
import kotlin.math.min

@Composable
fun PowerButton(
    state: VpnState,
    bootstrapPercent: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val errorColor = MaterialTheme.colorScheme.error

    // Pulse during Connecting; soft glow when Connected.
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse-scale"
    )
    val breath by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "breath"
    )

    val targetScale = when (state) {
        VpnState.Connecting -> pulse
        VpnState.Connected -> 1f + breath * 0.02f
        else -> 1f
    }
    val scale by animateFloatAsState(targetScale, tween(400), label = "scale")

    val ringAlpha by animateFloatAsState(
        when (state) {
            VpnState.Connected -> 1f
            VpnState.Connecting -> 0.55f + breath * 0.35f
            VpnState.Error -> 0.3f
            else -> 0.4f
        },
        tween(500), label = "ring-alpha"
    )

    val progress = bootstrapPercent / 100f
    val animatedProgress by animateFloatAsState(progress, tween(450), label = "progress")

    Box(
        modifier = modifier
            .size(240.dp)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val r = min(size.width, size.height) / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val ringStroke = 4.dp.toPx()

            // outer ring
            drawCircle(
                color = outline.copy(alpha = ringAlpha * 0.55f),
                radius = r - ringStroke * 2,
                center = Offset(cx, cy),
                style = Stroke(width = ringStroke)
            )

            // bootstrap progress arc
            if (state == VpnState.Connecting && progress > 0f) {
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(cx - (r - ringStroke * 2), cy - (r - ringStroke * 2)),
                    size = androidx.compose.ui.geometry.Size(
                        (r - ringStroke * 2) * 2,
                        (r - ringStroke * 2) * 2
                    ),
                    style = Stroke(width = ringStroke * 1.6f)
                )
            }

            // inner power disc
            drawCircle(
                color = when (state) {
                    VpnState.Connected -> primary
                    VpnState.Error -> errorColor
                    else -> primary.copy(alpha = 0.85f)
                },
                radius = (r * 0.55f) * scale,
                center = Offset(cx, cy)
            )
            // power glyph (vertical bar + 3/4 circle)
            val glyphR = r * 0.22f * scale
            drawCircle(
                color = background,
                radius = glyphR * 0.92f,
                center = Offset(cx, cy),
                style = Stroke(width = 3.4.dp.toPx())
            )
            drawRect(
                color = background,
                topLeft = Offset(cx - 2.dp.toPx(), cy - glyphR - 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(4.dp.toPx(), glyphR * 1.2f)
            )
        }
    }
}
