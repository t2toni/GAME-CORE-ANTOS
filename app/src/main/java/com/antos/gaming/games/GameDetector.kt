package com.antos.gaming.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameDetector(private val context: Context) {
    suspend fun getInstalledGames(): List<GameApp> = withContext(Dispatchers.IO) {
        getAllApps().filter { app ->
            isAppAGame(app.packageName)
        }
    }

    suspend fun getAllApps(): List<GameApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val apps = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        val appList = mutableListOf<GameApp>()
        
        for (resolveInfo in apps) {
            val packageName = resolveInfo.activityInfo.packageName
            // Exclude our own app
            if (packageName == context.packageName) continue
            
            val appName = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            
            appList.add(GameApp(packageName, appName, icon))
        }
        
        appList.sortedBy { it.name }
    }
    
    private fun isAppAGame(packageName: String): Boolean {
        return try {
            val ai = context.packageManager.getApplicationInfo(packageName, 0)
            (ai.category == ApplicationInfo.CATEGORY_GAME) || ((ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0)
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAppByPackage(packageName: String): GameApp? {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            val appName = ai.loadLabel(pm).toString()
            val icon = ai.loadIcon(pm)
            GameApp(packageName, appName, icon)
        } catch (e: Exception) {
            null
        }
    }
    
    fun launchGame(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        }
        return false
    }
}
