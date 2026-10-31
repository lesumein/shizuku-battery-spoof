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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

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
 * 로비 중앙의 배터리 모양 게이지.
 * 표시 잔량만큼 액체가 차오르고(물결 애니메이션), 실제 잔량은 점선 눈금으로 보여 준다.
 */
@Composable
fun BatteryCore(
    displayLevel: Int?,
    actualLevel: Int?,
    charging: Boolean,
    overridden: Boolean,
    modifier: Modifier = Modifier,
) {
    val shown by animateFloatAsState((displayLevel ?: 0) / 100f, tween(900), label = "level")
    val actual by animateFloatAsState((actualLevel ?: 0) / 100f, tween(900), label = "actual")
    val anim = rememberInfiniteTransition(label = "battery")
    val phase by anim.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (charging) 1100 else 2600, easing = LinearEasing)),
        label = "wave",
    )
    val glow by anim.animateFloat(
        initialValue = 0.12f,
        targetValue = if (charging) 0.38f else 0.2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow",
    )
    val color = levelColor(displayLevel)
    val outline = Color.White.copy(alpha = 0.85f)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.56f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val capH = h * 0.06f
                val capW = w * 0.38f
                val bodyTop = capH
                val bodyH = h - capH
                val stroke = w * 0.035f
                val corner = CornerRadius(w * 0.14f)

                // 후광
                drawRoundRect(
                    color = color.copy(alpha = glow),
                    topLeft = Offset(-w * 0.05f, bodyTop - w * 0.05f),
                    size = Size(w * 1.1f, bodyH + w * 0.1f),
                    cornerRadius = CornerRadius(w * 0.2f),
                )
                // 몸체 배경과 단자
                drawRoundRect(Color(0xCC061118), Offset(0f, bodyTop), Size(w, bodyH), corner)
                drawRoundRect(
                    outline,
                    Offset((w - capW) / 2, 0f),
                    Size(capW, capH + stroke),
                    CornerRadius(capH * 0.4f),
                )

                // 내용물 (안쪽 영역으로 잘라 그림)
                val pad = stroke * 1.9f
                val innerTop = bodyTop + pad
                val innerBottom = h - pad
                val innerH = innerBottom - innerTop
                val inner = Path().apply {
                    addRoundRect(RoundRect(pad, innerTop, w - pad, innerBottom, CornerRadius(w * 0.09f)))
                }
                clipPath(inner) {
                    val surface = innerBottom - innerH * shown
                    val amp = if (shown in 0.01f..0.99f) innerH * 0.014f else 0f
                    val liquid = Path().apply {
                        moveTo(0f, innerBottom)
                        lineTo(0f, surface)
                        val steps = 40
                        for (i in 0..steps) {
                            val x = w * i / steps
                            lineTo(x, surface + amp * sin(i.toFloat() / steps * 2f * PI.toFloat() * 1.5f + phase))
                        }
                        lineTo(w, innerBottom)
                        close()
                    }
                    drawPath(
                        liquid,
                        Brush.verticalGradient(
                            listOf(color, color.copy(alpha = 0.45f)),
                            startY = surface,
                            endY = innerBottom,
                        ),
                    )
                    // 5칸 눈금
                    for (i in 1..4) {
                        val y = innerTop + innerH * i / 5
                        drawLine(Color.Black.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), strokeWidth = stroke * 0.4f)
                    }
                    // 실제 잔량 표시선
                    if (actualLevel != null) {
                        val y = innerBottom - innerH * actual
                        drawLine(
                            Color.White.copy(alpha = 0.9f),
                            Offset(0f, y),
                            Offset(w, y),
                            strokeWidth = stroke * 0.35f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke, stroke * 0.8f)),
                        )
                    }
                }

                // 외곽선
                drawRoundRect(
                    outline,
                    Offset(stroke / 2, bodyTop + stroke / 2),
                    Size(w - stroke, bodyH - stroke),
                    corner,
                    style = Stroke(stroke),
                )
            }

            val shadow = Shadow(Color.Black.copy(alpha = 0.8f), blurRadius = 12f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (charging) {
                    Text("⚡", fontSize = 30.sp, color = Color.White, style = TextStyle(shadow = shadow))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        displayLevel?.toString() ?: "--",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = TextStyle(shadow = shadow),
                    )
                    Text(
                        "%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = TextStyle(shadow = shadow),
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Text(
                    "실제 ${actualLevel?.let { "$it%" } ?: "-"}",
                    style = MaterialTheme.typography.labelLarge.copy(shadow = shadow),
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
        if (overridden) {
            Row(
                Modifier
                    .padding(top = 10.dp)
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
