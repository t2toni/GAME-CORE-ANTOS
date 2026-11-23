package com.antos.gaming.games

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameDetectorTest {

    private val mockContext = mock<Context>()
    private val mockPackageManager = mock<PackageManager>()

    @Before
    fun setup() {
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockContext.packageName).thenReturn("com.antos.gaming")
    }

    @Test
    fun `getInstalledGames excludes own app and returns mapped GameApps`() = runBlocking {
        // Setup mock ResolveInfo for a dummy game
        val mockGameResolveInfo = mock<ResolveInfo>()
        val mockGameActivityInfo = ActivityInfo().apply { packageName = "com.test.game" }
        mockGameResolveInfo.activityInfo = mockGameActivityInfo
        whenever(mockGameResolveInfo.loadLabel(mockPackageManager)).thenReturn("Test Game")
        whenever(mockGameResolveInfo.loadIcon(mockPackageManager)).thenReturn(mock())

        // Setup mock ResolveInfo for own app
        val mockOwnResolveInfo = mock<ResolveInfo>()
        val mockOwnActivityInfo = ActivityInfo().apply { packageName = "com.antos.gaming" }
        mockOwnResolveInfo.activityInfo = mockOwnActivityInfo

        // Mock PM to return both
        whenever(mockPackageManager.queryIntentActivities(any(), any<Int>())).thenReturn(
            listOf(mockGameResolveInfo, mockOwnResolveInfo)
        )
        val appInfo = android.content.pm.ApplicationInfo().apply {
            category = android.content.pm.ApplicationInfo.CATEGORY_GAME
        }
        whenever(mockPackageManager.getApplicationInfo("com.test.game", 0)).thenReturn(appInfo)

        val detector = GameDetector(mockContext)
        val games = detector.getInstalledGames()

        assertEquals(1, games.size)
        assertEquals("com.test.game", games[0].packageName)
        assertEquals("Test Game", games[0].name)
    }

    @Test
    fun `launchGame starts activity if intent exists`() {
        val mockIntent = mock<Intent>()
        whenever(mockPackageManager.getLaunchIntentForPackage("com.test.game")).thenReturn(mockIntent)

        val detector = GameDetector(mockContext)
        val result = detector.launchGame("com.test.game")

        assertTrue(result)
    }
}
