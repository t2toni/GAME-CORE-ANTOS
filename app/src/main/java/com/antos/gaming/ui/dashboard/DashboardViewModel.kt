package com.antos.gaming.ui.dashboard

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antos.gaming.data.PreferencesRepository
import com.antos.gaming.games.GameApp
import com.antos.gaming.games.GameDetector
import com.antos.gaming.monitoring.MonitoringManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val gameDetector = GameDetector(application)
    private val monitoringManager = MonitoringManager.getInstance(application)
    private val preferencesRepository = PreferencesRepository(application)

    val batteryLevel: StateFlow<Int> = monitoringManager.batteryLevel
    val temperature: StateFlow<Float> = monitoringManager.temperature
    val availableMemoryGb: StateFlow<Float> = monitoringManager.availableMemoryGb
    val latency: StateFlow<Long> = monitoringManager.latency
    val networkType: StateFlow<String> = monitoringManager.networkType
    val fps: StateFlow<Int> = monitoringManager.fpsState
        .map { it.fps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val fpsHistory: StateFlow<List<Int>> = monitoringManager.fpsState
        .map { it.history }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _games = MutableStateFlow<List<GameApp>>(emptyList())
    val games: StateFlow<List<GameApp>> = _games.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val settingsManager = com.antos.gaming.settings.SettingsManager(application)
    val performanceMode: StateFlow<com.antos.gaming.settings.PerformanceMode> = settingsManager.performanceModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.antos.gaming.settings.PerformanceMode.BALANCED)

    fun setPerformanceMode(mode: com.antos.gaming.settings.PerformanceMode) {
        viewModelScope.launch {
            settingsManager.setPerformanceMode(mode)
        }
    }

    init {
        loadGames()
        monitoringManager.startMonitoring()
    }
    
    override fun onCleared() {
        super.onCleared()
        monitoringManager.stopMonitoring()
    }

    private fun loadGames() {
        viewModelScope.launch {
            _isLoading.value = true
            
            preferencesRepository.manualGamesFlow.collectLatest { manualPackages ->
                val autoGames = gameDetector.getInstalledGames()
                val manualGames = manualPackages.mapNotNull { pkg ->
                    gameDetector.getAppByPackage(pkg)
                }
                
                val combined = (autoGames + manualGames)
                    .distinctBy { it.packageName }
                    .sortedBy { it.name }
                
                _games.value = combined
                _isLoading.value = false
            }
        }
    }

    fun launchGame(packageName: String) {
        val launched = gameDetector.launchGame(packageName)
        if (launched) {
            val app = getApplication<Application>()
            val intent = Intent(app, com.antos.gaming.service.OverlayService::class.java).apply {
                putExtra(com.antos.gaming.service.OverlayService.EXTRA_GAME_PACKAGE, packageName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        }
    }
}
