package com.antos.gaming.ui.sidebar

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antos.gaming.MainActivity
import com.antos.gaming.data.PreferencesRepository
import com.antos.gaming.games.GameApp
import com.antos.gaming.games.GameDetector
import com.antos.gaming.recording.MediaRequestActivity
import com.antos.gaming.service.GamingModeManager
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AntosBlack = Color(0xFF03030A)
private val AntosPanel = Color(0xFF080814)
private val Violet = Color(0xFF9D3CFF)
private val BrightViolet = Color(0xFFC46BFF)
private val DeepViolet = Color(0xFF4C12B8)
private val Cyan = Color(0xFF20D9FF)
private val White = Color(0xFFF4ECFF)
private val Muted = Color(0xFF9E8DB5)

fun Modifier.hudBorder(): Modifier =
    this
        .border(
            width = 1.dp,
            color = Violet.copy(alpha = .75f),
            shape = CutCornerShape(14.dp)
        )
        .background(
            AntosPanel.copy(alpha = .94f),
            CutCornerShape(14.dp)
        )

@Composable
fun CyberGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        val gridColor = Violet.copy(alpha = 0.05f)
        for (x in 0..(size.width / step).toInt()) {
            drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, size.height), strokeWidth = 1f)
        }
        for (y in 0..(size.height / step).toInt()) {
            drawLine(gridColor, Offset(0f, y * step), Offset(size.width, y * step), strokeWidth = 1f)
        }
    }
}

@Composable
fun OverlayComposeView(
    fps: Int,
    minFps: Int = 0,
    maxFps: Int = 0,
    history: List<Int> = emptyList(),
    temp: Float,
    battery: Int,
    ram: Float,
    latency: Long,
    networkType: String,
    onClose: () -> Unit,
    updatePosition: (Int, Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val gamingModeManager = remember { GamingModeManager(context) }
    val settingsManager = remember { com.antos.gaming.settings.SettingsManager(context) }
    val preferencesRepository = remember { PreferencesRepository(context) }
    val gameDetector = remember { GameDetector(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val popupApps by preferencesRepository.popupAppsFlow.collectAsState(initial = PreferencesRepository.DEFAULT_POPUP_APPS)
    var installedApps by remember { mutableStateOf<List<GameApp>>(emptyList()) }
    var showAppPickerModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        installedApps = gameDetector.getAllApps()
    }

    var dndActive by remember { mutableStateOf(false) }
    val showNormalFps by settingsManager.showNormalFpsFlow.collectAsState(initial = true)
    val showAccurateFps by settingsManager.showAccurateFpsFlow.collectAsState(initial = false)
    val performanceMode by settingsManager.performanceModeFlow.collectAsState(initial = com.antos.gaming.settings.PerformanceMode.BALANCED)
    val monitoringManager = remember { com.antos.gaming.monitoring.MonitoringManager.getInstance(context) }
    val normalFpsState by monitoringManager.normalFpsState.collectAsState()
    val accurateFpsState by monitoringManager.accurateFpsState.collectAsState()
    var isAccurateActive by remember { mutableStateOf(com.antos.gaming.monitoring.ScreenCaptureService.isFpsActive) }

    LaunchedEffect(Unit) {
        while (true) {
            isAccurateActive = com.antos.gaming.monitoring.ScreenCaptureService.isFpsActive
            kotlinx.coroutines.delay(1000)
        }
    }

    var cpuFreqStr by remember { mutableStateOf("N/A") }
    var cpuUsagePercent by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val curFile = java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
                val maxFile = java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
                var curFreqKhz = 0L
                var maxFreqKhz = 1L
                
                if (curFile.exists()) {
                    curFreqKhz = curFile.readText().trim().toLong()
                }
                if (maxFile.exists()) {
                    maxFreqKhz = maxFile.readText().trim().toLong()
                }
                
                if (curFreqKhz > 0) {
                    cpuFreqStr = String.format(java.util.Locale.US, "%.2f GHz", curFreqKhz / 1000000.0)
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

    Row(
        modifier = Modifier.wrapContentSize(),
        verticalAlignment = Alignment.Top
    ) {
        // Floating handle - subtle edge panel
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(if (isExpanded) Color.Transparent else White.copy(alpha = 0.2f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 5) {
                            isExpanded = true
                        } else if (dragAmount < -5) {
                            isExpanded = false
                        }
                    }
                }
                .clickable { isExpanded = !isExpanded }
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth },
            exit = slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth }
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(360.dp)
                    .height(750.dp)
                    .hudBorder()
            ) {
                CyberGridOverlay()

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ANTOS", color = BrightViolet, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text("G A M I N G   C O R E", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Game Mode",
                                tint = com.antos.gaming.ui.theme.ErrorRed,
                                modifier = Modifier
                                    .clickable { onClose() }
                                    .padding(4.dp)
                                    .size(26.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Collapse",
                                tint = BrightViolet,
                                modifier = Modifier.clickable { isExpanded = false }.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // SYSTEM STATS ROW
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OverlayGauge("CPU", "${(cpuUsagePercent * 100).toInt()}%", cpuFreqStr, cpuUsagePercent, Modifier.weight(1f))
                                OverlayGauge("GPU", "67%", "650 MHz", .67f, Modifier.weight(1f))
                                OverlayGauge("RAM", "${(ram / 12f * 100).toInt()}%", "${String.format(java.util.Locale.US, "%.1f", ram)}G", (ram/12f).coerceIn(0f, 1f), Modifier.weight(1f))
                            }
                        }
                        
                        item {
                            // FPS AND TEMP
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f).height(120.dp).hudBorder().padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("SCREEN FPS", color = BrightViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        if (isAccurateActive) {
                                            Text("ACCU ON", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text("${normalFpsState.fps}", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text("NORM (LIVE)", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                            if (isAccurateActive) {
                                                Text("ACCU: ${accurateFpsState.fps}", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    val activeHistory = if (isAccurateActive && accurateFpsState.history.isNotEmpty()) accurateFpsState.history else normalFpsState.history
                                    OverlayMiniGraph(if (isAccurateActive) Cyan else Violet, activeHistory)
                                }
                                Column(modifier = Modifier.weight(1f).height(120.dp).hudBorder().padding(10.dp)) {
                                    Text("DEVICE STATS", color = BrightViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    OverlayTempRow("TEMP", "${temp.roundToInt()}°C", temp / 80f)
                                    OverlayTempRow("BAT", "$battery%", battery / 100f)
                                    OverlayTempRow("PING", "${latency}ms", (latency.toFloat() / 200f).coerceIn(0f, 1f))
                                }
                            }
                        }

                        item {
                            // PERFORMANCE ROTARY DIAL
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .hudBorder()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("PERFORMANCE MATRIX", color = BrightViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    val badgeColor = when (performanceMode) {
                                        com.antos.gaming.settings.PerformanceMode.BATTERY_SAVER -> Color(0xFF00FF9D)
                                        com.antos.gaming.settings.PerformanceMode.BALANCED -> Cyan
                                        com.antos.gaming.settings.PerformanceMode.MONSTER -> Color(0xFFFF2A6D)
                                    }
                                    Text(
                                        text = performanceMode.label,
                                        color = badgeColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                com.antos.gaming.ui.components.SciFiRotarySlider(
                                    currentMode = performanceMode,
                                    onModeSelected = { mode ->
                                        coroutineScope.launch {
                                            settingsManager.setPerformanceMode(mode)
                                        }
                                    },
                                    dialSize = 150.dp,
                                    showPills = true
                                )
                            }
                        }

                        item {
                            // ACTIONS ROW
                            Column(modifier = Modifier.fillMaxWidth().hudBorder().padding(12.dp)) {
                                Text("QUICK CONTROLS", color = BrightViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    var isRecordingActive by remember { mutableStateOf(false) }
                                    OverlayActionBtn(Icons.Default.Videocam, "RECORD", isRecordingActive, modifier = Modifier.weight(1f)) {
                                        if (isRecordingActive) {
                                            isRecordingActive = false
                                            val intent = Intent(context, com.antos.gaming.monitoring.ScreenCaptureService::class.java).apply {
                                                action = com.antos.gaming.monitoring.ScreenCaptureService.ACTION_STOP_RECORD
                                            }
                                            context.startService(intent)
                                        } else {
                                            isRecordingActive = true
                                            val intent = Intent(context, MediaRequestActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(intent)
                                        }
                                    }
                                    OverlayActionBtn(Icons.Default.Notifications, "DND", dndActive, modifier = Modifier.weight(1f)) {
                                        dndActive = !dndActive
                                        gamingModeManager.toggleGamingMode()
                                    }
                                    OverlayActionBtn(Icons.Default.Speed, "NORM FPS", showNormalFps, modifier = Modifier.weight(1f)) {
                                        val newValue = !showNormalFps
                                        coroutineScope.launch {
                                            settingsManager.saveSetting(com.antos.gaming.settings.SettingsManager.SHOW_NORMAL_FPS, newValue)
                                        }
                                    }
                                    OverlayActionBtn(Icons.Default.QueryStats, "ACCU FPS", showAccurateFps || isAccurateActive, modifier = Modifier.weight(1f)) {
                                        val newValue = !(showAccurateFps || isAccurateActive)
                                        coroutineScope.launch {
                                            settingsManager.saveSetting(com.antos.gaming.settings.SettingsManager.SHOW_ACCURATE_FPS, newValue)
                                        }
                                        if (newValue) {
                                            val intent = Intent(context, MediaRequestActivity::class.java).apply { 
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) 
                                                putExtra("TARGET_SERVICE", "FPS_MONITOR")
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            val intent = Intent(context, com.antos.gaming.monitoring.ScreenCaptureService::class.java).apply {
                                                action = com.antos.gaming.monitoring.ScreenCaptureService.ACTION_STOP
                                            }
                                            context.startService(intent)
                                        }
                                    }
                                    OverlayActionBtn(Icons.Default.Settings, "DASH", false, modifier = Modifier.weight(1f)) {
                                        val intent = Intent(context, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        }
                        
                        item {
                            // POP-UP APPS SECTION
                            Column(modifier = Modifier.fillMaxWidth().hudBorder().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("POP-UP APPS", color = BrightViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "+ MANAGE",
                                        color = Cyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { showAppPickerModal = true }
                                            .padding(4.dp)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(popupApps, key = { it }) { pkg ->
                                        val app = installedApps.find { it.packageName == pkg }
                                        val title = app?.name ?: when (pkg) {
                                            "com.discord" -> "DISCORD"
                                            "com.brave.browser" -> "BRAVE"
                                            "com.instagram.android" -> "INSTAGRAM"
                                            else -> pkg.substringAfterLast(".").uppercase()
                                        }

                                        PopupAppItem(
                                            title = title,
                                            icon = app?.icon,
                                            defaultIcon = when (pkg) {
                                                "com.discord" -> Icons.Default.Chat
                                                "com.brave.browser" -> Icons.Default.Language
                                                "com.instagram.android" -> Icons.Default.CameraAlt
                                                else -> Icons.Default.Apps
                                            },
                                            onClick = { openAppInPopup(context, pkg) },
                                            onDelete = {
                                                coroutineScope.launch {
                                                    preferencesRepository.removePopupApp(pkg)
                                                }
                                            }
                                        )
                                    }

                                    item {
                                        AddAppOverlayButton(
                                            onClick = { showAppPickerModal = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // In-Overlay App Picker Modal
                if (showAppPickerModal) {
                    AppPickerOverlayModal(
                        installedApps = installedApps,
                        selectedPackages = popupApps,
                        onAddApp = { pkg ->
                            coroutineScope.launch {
                                preferencesRepository.addPopupApp(pkg)
                            }
                        },
                        onRemoveApp = { pkg ->
                            coroutineScope.launch {
                                preferencesRepository.removePopupApp(pkg)
                            }
                        },
                        onDismiss = { showAppPickerModal = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopupAppItem(
    title: String,
    icon: android.graphics.drawable.Drawable?,
    defaultIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CutCornerShape(8.dp))
            .background(AntosPanel)
            .border(1.dp, Violet, CutCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                androidx.compose.foundation.Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = title,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    imageVector = defaultIcon,
                    contentDescription = title,
                    tint = BrightViolet,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = Muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddAppOverlayButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(70.dp)
            .clip(CutCornerShape(8.dp))
            .background(Violet.copy(alpha = 0.15f))
            .border(1.dp, Cyan.copy(alpha = 0.8f), CutCornerShape(8.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add App",
            tint = Cyan,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "ADD APP",
            color = Cyan,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppPickerOverlayModal(
    installedApps: List<GameApp>,
    selectedPackages: List<String>,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(query, installedApps) {
        if (query.isBlank()) installedApps
        else installedApps.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AntosBlack.copy(alpha = 0.98f))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("POP-UP APPS", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Select apps to launch in floating windows", color = Muted, fontSize = 10.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = White)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                placeholder = { Text("Search installed apps...", color = Muted, fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = BrightViolet, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = Violet.copy(alpha = 0.5f),
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = Cyan
                ),
                shape = CutCornerShape(8.dp)
            )

            Spacer(Modifier.height(12.dp))

            // App list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isAdded = selectedPackages.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CutCornerShape(8.dp))
                            .background(if (isAdded) Violet.copy(alpha = 0.2f) else AntosPanel)
                            .border(1.dp, if (isAdded) Cyan.copy(alpha = 0.6f) else Violet.copy(alpha = 0.3f), CutCornerShape(8.dp))
                            .clickable {
                                if (isAdded) onRemoveApp(app.packageName)
                                else onAddApp(app.packageName)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = rememberDrawablePainter(app.icon),
                            contentDescription = app.name,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                color = White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                color = Muted,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isAdded) onRemoveApp(app.packageName)
                                else onAddApp(app.packageName)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdded) com.antos.gaming.ui.theme.ErrorRed.copy(alpha = 0.8f) else Cyan.copy(alpha = 0.25f),
                                contentColor = White
                            ),
                            shape = CutCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (isAdded) "REMOVE" else "+ ADD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdded) White else Cyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayGauge(title: String, value: String, sub: String, progress: Float, modifier: Modifier) {
    Column(modifier = modifier.height(140.dp).hudBorder().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = BrightViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 4.dp,
                color = Cyan,
                trackColor = DeepViolet.copy(alpha = .3f)
            )
            Text(value, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(sub, color = Muted, fontSize = 9.sp)
    }
}

@Composable
fun OverlayMiniGraph(color: Color, data: List<Int>, maxValue: Float = 60f) {
    Canvas(modifier = Modifier.fillMaxWidth().height(25.dp)) {
        val points = if (data.isEmpty()) listOf(0f) else data.map { it.toFloat() / maxValue }
        
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.size.coerceAtLeast(2) - 1)
            val y = size.height * (1f - value.coerceIn(0f, 1f))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun OverlayTempRow(name: String, value: String, progress: Float) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.width(35.dp), color = Muted, fontSize = 9.sp)
        Text(value, modifier = Modifier.width(35.dp), color = White, fontSize = 9.sp)
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(3.dp),
            color = Cyan,
            trackColor = DeepViolet.copy(alpha = .3f)
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun OverlayActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(CutCornerShape(8.dp))
            .background(if (active) Violet else AntosPanel)
            .border(1.dp, if (active) BrightViolet else Violet, CutCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if(active) White else BrightViolet, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            text = title,
            color = if(active) White else Muted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
