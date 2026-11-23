package com.antos.gaming.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antos.gaming.settings.PerformanceMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val DarkDialBg = Color(0xFF090818)
private val KnurledBorder = Color(0xFF281850)
private val SaverGreen = Color(0xFF00FF9D)
private val BalancedCyan = Color(0xFF20D9FF)
private val MonsterRed = Color(0xFFFF2A6D)
private val MonsterFlame = Color(0xFFFF6200)

fun triggerRotaryHaptic(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    } catch (_: Exception) {}
}

@Composable
fun SciFiRotarySlider(
    currentMode: PerformanceMode,
    onModeSelected: (PerformanceMode) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 200.dp,
    showPills: Boolean = true
) {
    val context = LocalContext.current
    
    // Angles:
    // BATTERY_SAVER = 150 deg (lower-left)
    // BALANCED = 270 deg (top apex)
    // MONSTER = 390 deg (lower-right, 30 deg normalized)
    val targetAngle = when (currentMode) {
        PerformanceMode.BATTERY_SAVER -> 150f
        PerformanceMode.BALANCED -> 270f
        PerformanceMode.MONSTER -> 390f
    }

    var dragAngle by remember { mutableStateOf<Float?>(null) }
    
    val animatedAngle by animateFloatAsState(
        targetValue = dragAngle ?: targetAngle,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotaryAngle"
    )

    val activeColor by animateColorAsState(
        targetValue = when (currentMode) {
            PerformanceMode.BATTERY_SAVER -> SaverGreen
            PerformanceMode.BALANCED -> BalancedCyan
            PerformanceMode.MONSTER -> MonsterRed
        },
        animationSpec = tween(300),
        label = "activeColor"
    )

    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ROTARY DIAL CANVAS & KNOB
        Box(
            modifier = Modifier
                .size(dialSize)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val rad = atan2(offset.y - cy, offset.x - cx)
                            var deg = Math.toDegrees(rad.toDouble()).toFloat()
                            if (deg < 0) deg += 360f
                            if (deg < 90f) deg += 360f
                            val clamped = deg.coerceIn(150f, 390f)
                            dragAngle = clamped
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val rad = atan2(change.position.y - cy, change.position.x - cx)
                            var deg = Math.toDegrees(rad.toDouble()).toFloat()
                            if (deg < 0) deg += 360f
                            if (deg < 90f) deg += 360f
                            val clamped = deg.coerceIn(150f, 390f)
                            dragAngle = clamped
                        },
                        onDragEnd = {
                            val finalDeg = dragAngle ?: targetAngle
                            dragAngle = null
                            val selected = when {
                                finalDeg < 210f -> PerformanceMode.BATTERY_SAVER
                                finalDeg < 330f -> PerformanceMode.BALANCED
                                else -> PerformanceMode.MONSTER
                            }
                            if (selected != currentMode) {
                                triggerRotaryHaptic(context)
                                onModeSelected(selected)
                            }
                        },
                        onDragCancel = {
                            dragAngle = null
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val rad = atan2(offset.y - cy, offset.x - cx)
                        var deg = Math.toDegrees(rad.toDouble()).toFloat()
                        if (deg < 0) deg += 360f
                        if (deg < 90f) deg += 360f
                        val selected = when {
                            deg < 210f -> PerformanceMode.BATTERY_SAVER
                            deg < 330f -> PerformanceMode.BALANCED
                            else -> PerformanceMode.MONSTER
                        }
                        if (selected != currentMode) {
                            triggerRotaryHaptic(context)
                            onModeSelected(selected)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerRadius = (size.minDimension / 2f) - 10.dp.toPx()
                val knobRadius = outerRadius * 0.68f

                // 1. Draw outer background arc (from 150 deg to 390 deg -> 240 deg sweep)
                val strokeW = 8.dp.toPx()
                drawArc(
                    color = Color(0xFF1E143A),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // 2. Draw active glowing progress arc
                val currentSweep = (animatedAngle - 150f).coerceIn(0f, 240f)
                val activeBrush = when (currentMode) {
                    PerformanceMode.BATTERY_SAVER -> Brush.sweepGradient(
                        0.4f to SaverGreen.copy(alpha = 0.5f),
                        0.7f to SaverGreen,
                        center = Offset(cx, cy)
                    )
                    PerformanceMode.BALANCED -> Brush.sweepGradient(
                        0.4f to BalancedCyan.copy(alpha = 0.5f),
                        0.7f to BalancedCyan,
                        center = Offset(cx, cy)
                    )
                    PerformanceMode.MONSTER -> Brush.sweepGradient(
                        0.4f to MonsterRed,
                        0.75f to MonsterFlame,
                        center = Offset(cx, cy)
                    )
                }

                if (currentSweep > 1f) {
                    drawArc(
                        brush = activeBrush,
                        startAngle = 150f,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // 3. Draw tick marks along the 240-deg arc
                val tickCount = 21
                for (i in 0..tickCount) {
                    val angleDeg = 150f + (240f * i / tickCount)
                    val rad = angleDeg * PI / 180f
                    val isDetent = (i == 0 || i == tickCount / 2 || i == tickCount)
                    val tickLen = if (isDetent) 14.dp.toPx() else 8.dp.toPx()
                    val tickStroke = if (isDetent) 2.5.dp.toPx() else 1.2.dp.toPx()
                    
                    val rIn = outerRadius - 14.dp.toPx()
                    val rOut = rIn + tickLen

                    val startX = cx + (rIn * cos(rad)).toFloat()
                    val startY = cy + (rIn * sin(rad)).toFloat()
                    val endX = cx + (rOut * cos(rad)).toFloat()
                    val endY = cy + (rOut * sin(rad)).toFloat()

                    val isLit = angleDeg <= animatedAngle + 2f
                    val tickColor = if (isLit) activeColor else Color(0xFF483A6B)

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                }

                // 4. Draw outer Knurled ring
                drawCircle(
                    color = KnurledBorder,
                    radius = knobRadius + 4.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )

                // 5. Draw inner Knob body
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF221742), DarkDialBg, Color(0xFF06050E)),
                        center = Offset(cx, cy),
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = Offset(cx, cy)
                )

                // 6. Draw pointer / indicator needle pointing toward animatedAngle
                val needleRad = animatedAngle * PI / 180f
                val needleInnerR = knobRadius * 0.35f
                val needleOuterR = knobRadius - 3.dp.toPx()

                val nStartX = cx + (needleInnerR * cos(needleRad)).toFloat()
                val nStartY = cy + (needleInnerR * sin(needleRad)).toFloat()
                val nEndX = cx + (needleOuterR * cos(needleRad)).toFloat()
                val nEndY = cy + (needleOuterR * sin(needleRad)).toFloat()

                // Glowing needle line
                drawLine(
                    color = activeColor,
                    start = Offset(nStartX, nStartY),
                    end = Offset(nEndX, nEndY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Needle tip glowing pip
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(nEndX, nEndY)
                )

                // Center bezel ring
                drawCircle(
                    color = activeColor.copy(alpha = 0.4f),
                    radius = needleInnerR + 2.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Center Core Icon & Indicator Hub
            Box(
                modifier = Modifier
                    .size(dialSize * 0.28f)
                    .clip(CircleShape)
                    .background(DarkDialBg)
                    .border(1.5.dp, activeColor.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (currentMode) {
                    PerformanceMode.BATTERY_SAVER -> Icons.Default.BatterySaver
                    PerformanceMode.BALANCED -> Icons.Default.Speed
                    PerformanceMode.MONSTER -> Icons.Default.LocalFireDepartment
                }
                Icon(
                    imageVector = icon,
                    contentDescription = currentMode.label,
                    tint = activeColor,
                    modifier = Modifier.size(dialSize * 0.16f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Active Mode Badge Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(CutCornerShape(8.dp))
                .background(activeColor.copy(alpha = 0.12f))
                .border(1.dp, activeColor.copy(alpha = 0.5f), CutCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = currentMode.label,
                color = activeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        if (showPills) {
            Spacer(Modifier.height(8.dp))

            // 3 Mode Direct Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeSelectorPill(
                    title = "SAVER",
                    selected = currentMode == PerformanceMode.BATTERY_SAVER,
                    activeColor = SaverGreen,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentMode != PerformanceMode.BATTERY_SAVER) {
                        triggerRotaryHaptic(context)
                        onModeSelected(PerformanceMode.BATTERY_SAVER)
                    }
                }

                ModeSelectorPill(
                    title = "BALANCED",
                    selected = currentMode == PerformanceMode.BALANCED,
                    activeColor = BalancedCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentMode != PerformanceMode.BALANCED) {
                        triggerRotaryHaptic(context)
                        onModeSelected(PerformanceMode.BALANCED)
                    }
                }

                ModeSelectorPill(
                    title = "MONSTER",
                    selected = currentMode == PerformanceMode.MONSTER,
                    activeColor = MonsterRed,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentMode != PerformanceMode.MONSTER) {
                        triggerRotaryHaptic(context)
                        onModeSelected(PerformanceMode.MONSTER)
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectorPill(
    title: String,
    selected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(CutCornerShape(6.dp))
            .background(if (selected) activeColor.copy(alpha = 0.22f) else DarkDialBg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) activeColor else KnurledBorder,
                shape = CutCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else Color(0xFF9E8DB5),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}
