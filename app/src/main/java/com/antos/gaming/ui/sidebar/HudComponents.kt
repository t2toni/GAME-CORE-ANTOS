package com.antos.gaming.ui.sidebar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antos.gaming.ui.theme.DarkPanel
import com.antos.gaming.ui.theme.HudAccent
import com.antos.gaming.ui.theme.NearBlack
import com.antos.gaming.ui.theme.VioletMain
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults

// A shape with angled corners
fun hudShape(cutSize: Float = 12f) = GenericShape { size, _ ->
    moveTo(cutSize, 0f)
    lineTo(size.width - cutSize, 0f)
    lineTo(size.width, cutSize)
    lineTo(size.width, size.height - cutSize)
    lineTo(size.width - cutSize, size.height)
    lineTo(cutSize, size.height)
    lineTo(0f, size.height - cutSize)
    lineTo(0f, cutSize)
    close()
}

@Composable
fun HudContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(hudShape(16f))
            .background(DarkPanel)
            .border(1.dp, VioletMain.copy(alpha = 0.5f), hudShape(16f))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun HudMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) VioletMain.copy(alpha = 0.3f) else Color.Transparent
    val contentColor = if (isSelected) Color.White else VioletMain.copy(alpha = 0.8f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(hudShape(8f))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HudStatCard(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(hudShape(12f))
            .border(1.dp, VioletMain.copy(alpha = 0.4f), hudShape(12f))
            .background(NearBlack.copy(alpha = 0.6f))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, color = VioletMain.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
fun HudGridToggle(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) HudAccent else VioletMain.copy(alpha = 0.3f)
    val iconColor = if (isActive) HudAccent else VioletMain.copy(alpha = 0.7f)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(hudShape(10f))
            .background(NearBlack.copy(alpha = 0.5f))
            .border(1.dp, borderColor, hudShape(10f))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Spacer(modifier = Modifier.height(4.dp))
        // Mini toggle indicator
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(10.dp)
                .clip(hudShape(4f))
                .background(if (isActive) HudAccent else NearBlack)
                .border(1.dp, if (isActive) HudAccent else VioletMain.copy(alpha = 0.5f), hudShape(4f))
        )
    }
}

@Composable
fun HudCircularStat(
    label: String,
    value: Float,
    max: Float,
    textValue: String,
    color: Color
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
        val sweepAngle = (value / max) * 360f
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background track
            drawArc(
                color = color.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            // Gradient filled arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha=0.5f), color, color.copy(alpha=0.5f))
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = textValue, 
                color = Color.White, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            if (label.isNotEmpty()) {
                Text(text = label, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun HudBarStat(label: String, value: Float, max: Float, textValue: String, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (label.isNotEmpty()) {
                Text(text = label, color = VioletMain, style = MaterialTheme.typography.labelSmall)
            }
            if (textValue.isNotEmpty()) {
                Text(text = textValue, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 2.dp)) {
            val progress = (value / max).coerceIn(0f, 1f)
            drawRect(color = color.copy(alpha = 0.2f), size = size)
            
            val segmentWidth = 10.dp.toPx()
            val spacing = 2.dp.toPx()
            val activeWidth = size.width * progress
            var currentX = 0f
            while (currentX < size.width) {
                val isSegmentActive = currentX < activeWidth
                drawRect(
                    color = if (isSegmentActive) color else Color.Transparent,
                    topLeft = Offset(currentX, 0f),
                    size = Size(segmentWidth, size.height)
                )
                currentX += segmentWidth + spacing
            }
        }
    }
}

@Composable
fun HudVerticalGradientBar(
    label: String,
    value: Float,
    max: Float,
    textValue: String,
    icon: ImageVector,
    bottomIcon: ImageVector = icon
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = VioletMain, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = VioletMain, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = textValue, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))
        
        val progress = (value / max).coerceIn(0f, 1f)
        val gradient = Brush.verticalGradient(
            colors = listOf(Color.Red, Color.Yellow, Color.Cyan)
        )
        
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(120.dp)
                .clip(hudShape(6f))
                .background(NearBlack)
                .border(1.5.dp, VioletMain, hudShape(6f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .background(gradient)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${value.toInt()}°C", 
            color = Color.White, 
            style = MaterialTheme.typography.labelMedium, 
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Icon(imageVector = bottomIcon, contentDescription = null, tint = VioletMain, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun HudLineGraph(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = HudAccent
) {
    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas
        
        val maxData = dataPoints.maxOrNull() ?: 1f
        val minData = dataPoints.minOrNull() ?: 0f
        val range = (maxData - minData).coerceAtLeast(1f)
        
        val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)
        
        val path = Path().apply {
            dataPoints.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - ((value - minData) / range * size.height)
                
                if (index == 0) moveTo(x, y)
                else lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Fill area under graph
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}

@Composable
fun HudSlider(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = VioletMain, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.White, style = MaterialTheme.typography.labelMedium)
            Slider(
                value = value,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    thumbColor = HudAccent,
                    activeTrackColor = VioletMain,
                    inactiveTrackColor = NearBlack
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "${(value * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
