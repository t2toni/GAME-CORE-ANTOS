package com.antos.gaming.ui.dashboard

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antos.gaming.R
import com.antos.gaming.games.GameApp
import com.antos.gaming.service.GamingModeManager
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 10. COLORS (Sci-Fi Theme)
val BgColor = Color(0xFF020108)
val PanelBg = Color(0xFF070213)
val PanelSecondary = Color(0xFF100326)
val NeonPurple = Color(0xFFB300FF)
val DeepPurple = Color(0xFF4C008A)
val BrightMagenta = Color(0xFFFF00FF)
val NeonCyan = Color(0xFF00FFFF)
val TextColor = Color(0xFFF4ECFF)
val MutedText = Color(0xFF7A6B8F)

// Custom Shape for Cut Corners
val cutCornerShape = GenericShape { size, _ ->
    val cut = 15.dp.value * 2.5f // approximate pixel density factor for shape
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height - cut)
    lineTo(0f, cut)
    close()
}

fun Modifier.sciFiBorder(animated: Boolean = false): Modifier = composed {
    val alpha = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alphaPulse"
        )
        animatedAlpha
    } else 0.3f

    this
        .clip(cutCornerShape)
        .background(PanelBg.copy(alpha = 0.85f)) // Glassmorphism-like translucency
        .drawBehind {
            val cut = 15.dp.toPx()
            val strokeW = 1.5.dp.toPx()
            val path = Path().apply {
                moveTo(cut, 0f)
                lineTo(size.width - cut, 0f)
                lineTo(size.width, cut)
                lineTo(size.width, size.height - cut)
                lineTo(size.width - cut, size.height)
                lineTo(cut, size.height)
                lineTo(0f, size.height - cut)
                lineTo(0f, cut)
                close()
            }
            
            // Outer Glow
            drawPath(path = path, color = NeonPurple.copy(alpha = alpha), style = Stroke(width = strokeW * 4))
            // Inner sharp border
            drawPath(path = path, color = NeonPurple, style = Stroke(width = strokeW))
        }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAddGame: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableStateOf("HOME") }
    
    val fps by viewModel.fps.collectAsState()
    val fpsHistory by viewModel.fpsHistory.collectAsState()
    val battery by viewModel.batteryLevel.collectAsState()
    val temp by viewModel.temperature.collectAsState()
    val ram by viewModel.availableMemoryGb.collectAsState()
    val games by viewModel.games.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        SciFiGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(12.dp)
        ) {
            SciFiHeader(battery)

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // LEFT SIDEBAR
                SciFiSidebar(
                    selectedTab = selectedTab,
                    onTabSelect = { 
                        selectedTab = it 
                        if (it == "SETTINGS") onNavigateToSettings()
                    },
                    modifier = Modifier.weight(0.18f)
                )

                Spacer(Modifier.width(10.dp))

                // MAIN AREA
                Crossfade(targetState = selectedTab, modifier = Modifier.weight(0.82f)) { tab ->
                    when (tab) {
                        "HOME" -> SciFiMainContent(fps, fpsHistory, battery, temp, ram)
                        "GAMES" -> SciFiGamesContent(
                            games = games, 
                            isLoading = isLoading, 
                            onLaunch = { pkg -> 
                                viewModel.launchGame(pkg)
                                (context as? android.app.Activity)?.finish()
                            },
                            onAddGame = onNavigateToAddGame
                        )
                        "PERFORMANCE" -> {
                            val perfMode by viewModel.performanceMode.collectAsState()
                            SciFiPerformanceContent(
                                performanceMode = perfMode,
                                onModeSelect = { viewModel.setPerformanceMode(it) }
                            )
                        }
                        else -> Box(modifier = Modifier.fillMaxSize().sciFiBorder(animated = false), contentAlignment = Alignment.Center) {
                            Text("$tab - INCOMING TRANSMISSION...", color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SciFiHeader(battery: Int) {
    val dateFormat = SimpleDateFormat("HH:mm\nEEE, dd MMM", Locale.getDefault())
    val currentTime = remember { dateFormat.format(Date()) }
    val timeParts = currentTime.split("\n")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left Header Box
        Box(modifier = Modifier.weight(1f).fillMaxHeight().sciFiBorder().padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ANTOS", color = BrightMagenta, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Spacer(Modifier.width(16.dp))
                Text("G A M I N G   C O R E", color = NeonCyan, fontSize = 10.sp, letterSpacing = 6.sp)
            }
        }
        
        Spacer(Modifier.width(20.dp))
        
        // Right Header Box
        Box(modifier = Modifier.weight(0.4f).fillMaxHeight().sciFiBorder().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(timeParts.getOrElse(0) { "10:24" }, color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(timeParts.getOrElse(1) { "TUE, 26 AUG" }.uppercase(), color = MutedText, fontSize = 8.sp)
                }
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.Wifi, contentDescription = "WiFi", tint = NeonPurple, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Bluetooth, contentDescription = "BT", tint = NeonPurple, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.BatteryFull, contentDescription = "Batt", tint = TextColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("$battery%", color = TextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SciFiSidebar(selectedTab: String, onTabSelect: (String) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .sciFiBorder()
            .padding(10.dp)
    ) {
        SciFiNavItem("HOME", Icons.Default.Home, selectedTab == "HOME") { onTabSelect("HOME") }
        SciFiNavItem("GAMES", Icons.Default.SportsEsports, selectedTab == "GAMES") { onTabSelect("GAMES") }
        SciFiNavItem("PERFORMANCE", Icons.Default.Speed, selectedTab == "PERFORMANCE") { onTabSelect("PERFORMANCE") }
        SciFiNavItem("TOOLS", Icons.Default.Build, selectedTab == "TOOLS") { onTabSelect("TOOLS") }
        SciFiNavItem("SETTINGS", Icons.Default.Settings, selectedTab == "SETTINGS") { onTabSelect("SETTINGS") }

        Spacer(Modifier.weight(1f))
        
        // Tribal Sharp Logo
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ChangeHistory, contentDescription = null, tint = BrightMagenta, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(4.dp))
            Text("ANTOS", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("GAMING CORE", color = NeonPurple, fontSize = 8.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun SciFiNavItem(name: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .padding(vertical = 4.dp)
            .clip(cutCornerShape)
            .background(if (selected) NeonPurple.copy(alpha = 0.3f) else Color.Transparent)
            .border(width = if (selected) 1.dp else 0.dp, color = NeonCyan, shape = cutCornerShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = name, tint = if (selected) TextColor else NeonPurple, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            name, 
            color = if (selected) TextColor else MutedText, 
            fontSize = 10.sp, 
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun SciFiMainContent(fps: Int, fpsHistory: List<Int>, battery: Int, temp: Float, ram: Float) {
    var cpuFreqStr by remember { mutableStateOf("N/A") }
    var cpuUsagePercent by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val curFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
                val maxFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
                var curFreqKhz = 0L
                var maxFreqKhz = 1L
                
                if (curFile.exists()) {
                    curFreqKhz = curFile.readText().trim().toLong()
                }
                if (maxFile.exists()) {
                    maxFreqKhz = maxFile.readText().trim().toLong()
                }
                
                if (curFreqKhz > 0) {
                    cpuFreqStr = String.format(Locale.US, "%.2f GHz", curFreqKhz / 1000000.0)
                    cpuUsagePercent = (curFreqKhz.toFloat() / maxFreqKhz.toFloat()).coerceIn(0f, 1f)
                } else {
                    cpuFreqStr = "N/A"
                    cpuUsagePercent = 0f
                }
            } catch (e: Exception) {
                // Ignore exceptions
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ROW 1: Hero & Device Info
        Row(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .sciFiBorder(animated = true)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cyberpunk_city),
                    contentDescription = "Cyberpunk City",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(cutCornerShape)
                )
                // Overlay text
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    Column {
                        Text("PLAY HARDER", color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("PERFORM BETTER", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            // Device Info
            SciFiDeviceInfo(Modifier.weight(1f))
        }

        // ROW 2: 4 Metric Cards
        Row(modifier = Modifier.height(200.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val gpuFreq = "N/A"
            SciFiMetricCard("CPU", "${(cpuUsagePercent * 100).toInt()}%", "CPU CLOCK", cpuFreqStr, cpuUsagePercent, Icons.Default.Memory, emptyList(), Modifier.weight(1f))
            SciFiMetricCard("GPU", "67%", "GPU CLOCK", gpuFreq, 0.67f, Icons.Default.Gamepad, emptyList(), Modifier.weight(1f))
            SciFiMetricCard("RAM", "${((ram/12f)*100).toInt()}%", "RAM USAGE", "${String.format(Locale.US, "%.1f", ram)} / 12 GB", (ram/12f).coerceIn(0f, 1f), Icons.Default.Storage, emptyList(), Modifier.weight(1f))
            SciFiMetricCard("BATTERY", "$battery%", "CHARGING", "FAST", battery/100f, Icons.Default.BatteryChargingFull, emptyList(), Modifier.weight(1f))
        }

        // ROW 3: Bottom Panels
        Row(modifier = Modifier.height(190.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SciFiPerformanceGraph(Modifier.weight(1.3f).sciFiBorder(animated = true))
            SciFiNetworkPanel(Modifier.weight(0.9f).sciFiBorder())
            SciFiGamingMode(Modifier.weight(1f).sciFiBorder())
            Column(modifier = Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SciFiTempPanel(temp, Modifier.weight(1f).sciFiBorder())
                SciFiFpsPanel(fps, fpsHistory, Modifier.weight(1f).sciFiBorder(animated = true))
            }
        }
    }
}

@Composable
fun SciFiDeviceInfo(modifier: Modifier) {
    val manufacturer = Build.MANUFACTURER.uppercase()
    val model = Build.MODEL
    val stat = StatFs(Environment.getDataDirectory().path)
    val blockSize = stat.blockSizeLong
    val totalStorageGb = (stat.blockCountLong * blockSize) / (1024 * 1024 * 1024)
    val availableStorageGb = (stat.availableBlocksLong * blockSize) / (1024 * 1024 * 1024)
    val usedStorageGb = totalStorageGb - availableStorageGb

    Column(modifier = modifier.fillMaxHeight().sciFiBorder().padding(12.dp)) {
        Text("DEVICE INFO", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("$manufacturer $model", color = TextColor, fontSize = 10.sp)
                Text("ANDROID ${Build.VERSION.RELEASE}", color = MutedText, fontSize = 8.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Available Storage", color = MutedText, fontSize = 9.sp)
            Text("$usedStorageGb GB / $totalStorageGb GB", color = NeonPurple, fontSize = 9.sp)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (usedStorageGb.toFloat() / totalStorageGb.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = NeonPurple,
            trackColor = DeepPurple
        )
    }
}

@Composable
fun SciFiMetricCard(title: String, value: String, subTitle: String, subValue: String, progress: Float, icon: ImageVector, history: List<Int>, modifier: Modifier) {
    var animationTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(progress) { animationTarget = progress }
    val animatedProgress by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "gaugeAnimation"
    )

    Column(modifier = modifier.fillMaxHeight().sciFiBorder().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(title, color = TextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(4.dp))
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.aspectRatio(1f).fillMaxHeight()) {
                // Outer ring
                drawArc(color = NeonPurple.copy(alpha=0.2f), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(4.dp.toPx()))
                // Inner filled ring with glow
                drawArc(
                    brush = Brush.sweepGradient(listOf(NeonPurple, BrightMagenta, NeonCyan)),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(4.dp.toPx())
                )
            }
            Text(value, color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(4.dp))
        
        Text(subTitle, color = MutedText, fontSize = 8.sp)
        Text(subValue, color = TextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        SciFiMiniGraph(history)
    }
}

@Composable
fun SciFiMiniGraph(data: List<Int>, maxValue: Float = 60f) {
    Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
        val points = if (data.isEmpty()) listOf(0f) else data.map { it.toFloat() / maxValue }
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = size.width * i / (points.size.coerceAtLeast(2) - 1)
            val y = size.height * (1f - p.coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = NeonPurple, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
fun SciFiPerformanceGraph(modifier: Modifier) {
    Column(modifier = modifier.fillMaxHeight().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ShowChart, contentDescription = null, tint = TextColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("REAL-TIME PERFORMANCE", color = TextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val stepX = size.width / 20f
            val stepY = size.height / 4f
            
            // Grid
            for (i in 0..20) drawLine(color = NeonCyan.copy(alpha=0.1f), start = Offset(i*stepX, 0f), end = Offset(i*stepX, size.height))
            for (i in 0..4) drawLine(color = NeonCyan.copy(alpha=0.1f), start = Offset(0f, i*stepY), end = Offset(size.width, i*stepY))
            
            val cpuPath = Path()
            val gpuPath = Path()
            for (i in 0..20) {
                val x = size.width * i / 20f
                val y1 = size.height * (.3f + ((i * 7) % 11) / 50f)
                val y2 = size.height * (.6f + ((i * 5) % 9) / 50f)
                if (i == 0) { cpuPath.moveTo(x, y1); gpuPath.moveTo(x, y2) }
                else { cpuPath.lineTo(x, y1); gpuPath.lineTo(x, y2) }
            }
            drawPath(cpuPath, BrightMagenta, style = Stroke(1.5.dp.toPx()))
            drawPath(gpuPath, NeonCyan, style = Stroke(1.5.dp.toPx()))
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text("● CPU", color = BrightMagenta, fontSize = 8.sp)
            Spacer(Modifier.width(12.dp))
            Text("● GPU", color = NeonCyan, fontSize = 8.sp)
        }
    }
}

@Composable
fun SciFiNetworkPanel(modifier: Modifier) {
    Column(modifier = modifier.fillMaxHeight().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = TextColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("NETWORK", color = TextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Wi-Fi", color = TextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Connected", color = MutedText, fontSize = 9.sp)
            }
            Text("Connected", color = NeonCyan, fontSize = 9.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ping (Device)", color = BrightMagenta, fontSize = 10.sp)
            Text("42 ms", color = TextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("↓ 12.4 Mbps", color = NeonCyan, fontSize = 9.sp)
            Text("↑ 6.8 Mbps", color = BrightMagenta, fontSize = 9.sp)
        }
    }
}

@Composable
fun SciFiGamingMode(modifier: Modifier) {
    val context = LocalContext.current
    val gamingModeManager = remember { GamingModeManager(context) }
    var gamingMode by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxHeight().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Gamepad, contentDescription = null, tint = TextColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("GAMING MODE", color = TextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = gamingMode,
                onCheckedChange = { gamingMode = it; gamingModeManager.toggleGamingMode() },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonPurple)
            )
        }
        Spacer(Modifier.height(8.dp))
        SciFiGameOption("Notifications Blocked", gamingMode)
        SciFiGameOption("Keep Screen Awake", gamingMode)
        SciFiGameOption("Orientation Locked", gamingMode)
        SciFiGameOption("Touch Protection", false)
    }
}

@Composable
fun SciFiGameOption(text: String, enabled: Boolean) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (enabled) NeonPurple else DeepPurple), contentAlignment = Alignment.Center) {
            if (enabled) Text("✓", color = TextColor, fontSize = 8.sp)
        }
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (enabled) TextColor else MutedText, fontSize = 8.sp)
    }
}

@Composable
fun SciFiTempPanel(temp: Float, modifier: Modifier) {
    Column(modifier = modifier.fillMaxHeight().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Thermostat, contentDescription = null, tint = TextColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("TEMPERATURE", color = TextColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        SciFiTempRow("CPU", "42°C", 0.42f)
        SciFiTempRow("GPU", "38°C", 0.38f)
        SciFiTempRow("BATT", "${String.format("%.1f", temp)}°C", temp/100f)
    }
}

@Composable
fun SciFiTempRow(name: String, value: String, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(name, color = MutedText, fontSize = 8.sp, modifier = Modifier.width(30.dp))
        Text(value, color = TextColor, fontSize = 8.sp, modifier = Modifier.width(30.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(4.dp),
            color = NeonPurple,
            trackColor = DeepPurple
        )
    }
}

@Composable
fun SciFiFpsPanel(fps: Int, history: List<Int>, modifier: Modifier) {
    Column(modifier = modifier.fillMaxHeight().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = TextColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("FPS", color = TextColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$fps", color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("(Device)", color = NeonPurple, fontSize = 8.sp)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Refresh Rate", color = MutedText, fontSize = 8.sp)
                val context = LocalContext.current
                val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                val refreshRate = displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.refreshRate?.toInt() ?: 120
                Text("$refreshRate Hz", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        SciFiMiniGraph(history, 120f)
    }
}

@Composable
fun SciFiGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 40.dp.toPx()
        for (x in 0..(size.width / spacing).toInt()) {
            drawLine(color = NeonPurple.copy(alpha = .1f), start = Offset(x * spacing, 0f), end = Offset(x * spacing, size.height), strokeWidth = 1f)
        }
        for (y in 0..(size.height / spacing).toInt()) {
            drawLine(color = NeonPurple.copy(alpha = .1f), start = Offset(0f, y * spacing), end = Offset(size.width, y * spacing), strokeWidth = 1f)
        }
    }
}

@Composable
fun SciFiGamesContent(games: List<GameApp>, isLoading: Boolean, onLaunch: (String) -> Unit, onAddGame: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().sciFiBorder(animated = false).padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Gamepad, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "GAMES - READY FOR LINK", 
                color = NeonCyan, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold, 
                letterSpacing = 2.sp
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onAddGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple.copy(alpha = 0.2f),
                    contentColor = TextColor
                ),
                shape = cutCornerShape,
                modifier = Modifier
                    .height(40.dp)
                    .border(1.dp, NeonCyan, cutCornerShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Game", tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ADD GAME", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonPurple)
            }
        } else if (games.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("NO GAMES DETECTED.", color = MutedText, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(games) { game ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(cutCornerShape)
                            .background(PanelSecondary)
                            .border(1.dp, NeonPurple.copy(alpha = 0.5f), cutCornerShape)
                            .clickable { onLaunch(game.packageName) }
                            .padding(12.dp)
                    ) {
                        Image(
                            painter = rememberDrawablePainter(game.icon),
                            contentDescription = game.name,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = game.name,
                            color = TextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SciFiPerformanceContent(
    performanceMode: com.antos.gaming.settings.PerformanceMode,
    onModeSelect: (com.antos.gaming.settings.PerformanceMode) -> Unit
) {
    val scrollState = rememberScrollState()

    val modeColor = when (performanceMode) {
        com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> Color(0xFF00FF9D)
        com.antos.gaming.settings.PerformanceMode.BALANCED -> NeonCyan
        com.antos.gaming.settings.PerformanceMode.MONSTER -> Color(0xFFFF2A6D)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "PERFORMANCE ENGINE",
                    color = BrightMagenta,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "DYNAMIC CLOCK & THERMAL PROFILER",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CutCornerShape(6.dp))
                    .background(modeColor.copy(alpha = 0.15f))
                    .border(1.dp, modeColor, CutCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(modeColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = performanceMode.label,
                    color = modeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // HERO ROW: ROTARY SLIDER & TELEMETRY SPECS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LEFT: ROTARY SLIDER HERO CARD
            Box(
                modifier = Modifier
                    .weight(0.52f)
                    .sciFiBorder(animated = performanceMode == com.antos.gaming.settings.PerformanceMode.MONSTER)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "ROTARY SELECTOR",
                        color = NeonPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(6.dp))

                    com.antos.gaming.ui.components.SciFiRotarySlider(
                        currentMode = performanceMode,
                        onModeSelected = onModeSelect,
                        dialSize = 132.dp,
                        showPills = true
                    )
                }
            }

            // RIGHT: LIVE SPECIFICATIONS & PROFILE DETAILS
            Column(
                modifier = Modifier
                    .weight(0.48f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile Overview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sciFiBorder()
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "MODE PROFILE",
                            color = NeonPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (performanceMode) {
                                com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER ->
                                    "Reduces CPU/GPU clocks and throttles background drain to extend gaming battery life by up to 40%."
                                com.antos.gaming.settings.PerformanceMode.BALANCED ->
                                    "Dynamically balances frame rate stability and device thermals with adaptive core clock scaling."
                                com.antos.gaming.settings.PerformanceMode.MONSTER ->
                                    "UNLEASHED: Forces peak GPU frequencies, disables thermal downclocking, and optimizes touch latency."
                            },
                            color = TextColor,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        // Target Frame Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TARGET FPS", color = MutedText, fontSize = 9.sp)
                            Text(
                                when (performanceMode) {
                                    com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> "60 FPS CAP"
                                    com.antos.gaming.settings.PerformanceMode.BALANCED -> "90/120 FPS ADAPTIVE"
                                    com.antos.gaming.settings.PerformanceMode.MONSTER -> "MAX / UNCAPPED"
                                },
                                color = modeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Core Boost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CPU BOOST", color = MutedText, fontSize = 9.sp)
                            Text(
                                when (performanceMode) {
                                    com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> "POWER-SAVER (40%)"
                                    com.antos.gaming.settings.PerformanceMode.BALANCED -> "DYNAMIC (75%)"
                                    com.antos.gaming.settings.PerformanceMode.MONSTER -> "OVERDRIVE (100%)"
                                },
                                color = modeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Battery Rating
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("EST. BATTERY", color = MutedText, fontSize = 9.sp)
                            Text(
                                when (performanceMode) {
                                    com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> "5.5 - 6.5 HOURS"
                                    com.antos.gaming.settings.PerformanceMode.BALANCED -> "3.5 - 4.5 HOURS"
                                    com.antos.gaming.settings.PerformanceMode.MONSTER -> "2.0 - 2.8 HOURS"
                                },
                                color = modeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Performance Sliders Telemetry Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sciFiBorder()
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "HARDWARE ALLOCATION",
                            color = NeonPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        val perfProgress = when (performanceMode) {
                            com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> 0.40f
                            com.antos.gaming.settings.PerformanceMode.BALANCED -> 0.75f
                            com.antos.gaming.settings.PerformanceMode.MONSTER -> 1.0f
                        }

                        val thermalProgress = when (performanceMode) {
                            com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> 0.30f
                            com.antos.gaming.settings.PerformanceMode.BALANCED -> 0.60f
                            com.antos.gaming.settings.PerformanceMode.MONSTER -> 0.95f
                        }

                        val batterySaverProgress = when (performanceMode) {
                            com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> 0.95f
                            com.antos.gaming.settings.PerformanceMode.BALANCED -> 0.65f
                            com.antos.gaming.settings.PerformanceMode.MONSTER -> 0.25f
                        }

                        SciFiMeterRow("CPU / GPU OVERDRIVE", "${(perfProgress * 100).toInt()}%", perfProgress, modeColor)
                        SciFiMeterRow("THERMAL BUFFER", "${(thermalProgress * 100).toInt()}%", thermalProgress, NeonPurple)
                        SciFiMeterRow("POWER CONSERVATION", "${(batterySaverProgress * 100).toInt()}%", batterySaverProgress, Color(0xFF00FF9D))
                    }
                }
            }
        }
    }
}

@Composable
fun SciFiMeterRow(label: String, value: String, progress: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = PanelSecondary
        )
    }
}

