package com.antos.gaming.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antos.gaming.data.PreferencesRepository
import com.antos.gaming.games.GameApp
import com.antos.gaming.games.GameDetector
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

@Composable
fun AppPickerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val gameDetector = remember { GameDetector(context) }
    val preferencesRepository = remember { PreferencesRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var allApps by remember { mutableStateOf<List<GameApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val manualGames by preferencesRepository.manualGamesFlow.collectAsState(initial = emptySet())

    LaunchedEffect(Unit) {
        allApps = gameDetector.getAllApps()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        // Top Bar
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "SELECT APPS TO ADD AS GAMES",
                color = TextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().sciFiBorder(animated = false).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allApps) { app ->
                    val isAdded = manualGames.contains(app.packageName)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cutCornerShape)
                            .background(if (isAdded) NeonPurple.copy(alpha = 0.2f) else PanelSecondary)
                            .border(1.dp, if (isAdded) NeonCyan else NeonPurple.copy(alpha = 0.3f), cutCornerShape)
                            .clickable {
                                coroutineScope.launch {
                                    if (isAdded) {
                                        preferencesRepository.removeManualGame(app.packageName)
                                    } else {
                                        preferencesRepository.addManualGame(app.packageName)
                                    }
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Image(
                            painter = rememberDrawablePainter(app.icon),
                            contentDescription = app.name,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.name, color = TextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = app.packageName, color = MutedText, fontSize = 10.sp)
                        }
                        Checkbox(
                            checked = isAdded,
                            onCheckedChange = null, // Handled by Row click
                            colors = CheckboxDefaults.colors(
                                checkedColor = NeonCyan,
                                uncheckedColor = NeonPurple,
                                checkmarkColor = BgColor
                            )
                        )
                    }
                }
            }
        }
    }
}
