package io.github.lesumein.batteryspoof.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 반투명 유리 질감 패널 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = LobbyColors.GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = LobbyColors.Glass,
        border = BorderStroke(1.dp, borderColor),
    ) { content() }
}

/** 상태를 나타내는 빛나는 점 */
@Composable
fun GlowDot(color: Color, size: Dp = 10.dp, pulse: Boolean = false) {
    val alpha = if (pulse) {
        val t = rememberInfiniteTransition(label = "dot")
        t.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "dotAlpha",
        ).value
    } else 1f
    Box(
        Modifier
            .size(size)
            .alpha(alpha)
            .background(color, CircleShape)
            .border(2.dp, color.copy(alpha = 0.35f), CircleShape)
    )
}

fun levelColor(level: Int?): Color = when {
    level == null -> LobbyColors.TextDim
    level <= 15 -> LobbyColors.Danger
    level <= 40 -> LobbyColors.Amber
    else -> LobbyColors.Lime
}

/**
 * 로비 중앙의 원형 배터리 게이지.
 * 표시 잔량을 큰 숫자와 호(arc)로, 실제 잔량을 안쪽의 가는 호로 보여 준다.
 */
@Composable
fun BatteryCore(
    displayLevel: Int?,
    actualLevel: Int?,
    charging: Boolean,
    overridden: Boolean,
    modifier: Modifier = Modifier,
) {
    val shown by animateFloatAsState((displayLevel ?: 0) / 100f, tween(700), label = "level")
    val actual by animateFloatAsState((actualLevel ?: 0) / 100f, tween(700), label = "actual")
    val spin = rememberInfiniteTransition(label = "spin")
    val rotation by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (charging) 2400 else 9000, easing = LinearEasing)),
        label = "rotation",
    )
    val color = levelColor(displayLevel)

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = size.minDimension
            val stroke = outer * 0.06f
            val inset = stroke / 2 + outer * 0.04f
            val arcSize = Size(outer - inset * 2, outer - inset * 2)
            val topLeft = Offset(inset, inset)

            // 은은한 후광
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = 0.28f), Color.Transparent),
                    radius = outer / 2,
                ),
                radius = outer / 2,
            )
            // 회전하는 점선 링
            val dashes = 48
            for (i in 0 until dashes) {
                if (i % 2 == 1) continue
                drawArc(
                    color = LobbyColors.Cyan.copy(alpha = 0.35f),
                    startAngle = rotation + i * (360f / dashes),
                    sweepAngle = 360f / dashes * 0.6f,
                    useCenter = false,
                    topLeft = Offset(outer * 0.01f, outer * 0.01f),
                    size = Size(outer * 0.98f, outer * 0.98f),
                    style = Stroke(width = outer * 0.008f),
                )
            }
            // 트랙
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // 표시 잔량
            drawArc(
                brush = Brush.sweepGradient(listOf(LobbyColors.Cyan, color, LobbyColors.Cyan)),
                startAngle = 135f, sweepAngle = 270f * shown, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // 실제 잔량 (안쪽 가는 호)
            val innerInset = inset + stroke * 1.4f
            drawArc(
                color = Color.White.copy(alpha = 0.55f),
                startAngle = 135f, sweepAngle = 270f * actual, useCenter = false,
                topLeft = Offset(innerInset, innerInset),
                size = Size(outer - innerInset * 2, outer - innerInset * 2),
                style = Stroke(stroke * 0.25f, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (charging) Text("⚡", fontSize = 26.sp, color = LobbyColors.Lime)
            Text(
                displayLevel?.toString() ?: "--",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text("표시 잔량 %", style = MaterialTheme.typography.labelMedium, color = LobbyColors.TextDim)
            Text(
                "실제 ${actualLevel?.let { "$it%" } ?: "-"}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )
            if (overridden) {
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .background(LobbyColors.Amber.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .border(1.dp, LobbyColors.Amber.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GlowDot(LobbyColors.Amber, 7.dp, pulse = true)
                    Text("OVERRIDE", color = LobbyColors.Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** 하단 메뉴 타일 */
@Composable
fun MenuTile(
    glyph: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: Color = LobbyColors.Cyan,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    GlassPanel(
        modifier = modifier.alpha(if (enabled) 1f else 0.45f),
        borderColor = accent.copy(alpha = 0.45f),
        onClick = if (enabled) onClick else null,
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(glyph, fontSize = 22.sp, color = accent)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = LobbyColors.TextDim,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
