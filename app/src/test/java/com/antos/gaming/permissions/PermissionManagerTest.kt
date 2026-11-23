package com.antos.gaming.permissions

import android.content.Context
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PermissionManagerTest {

    @Test
    fun `getOverlayPermissionIntent generates correct intent`() {
        val mockContext = mock<Context>()
        whenever(mockContext.packageName).thenReturn("com.antos.gaming")

        val intent = PermissionManager.getOverlayPermissionIntent(mockContext)
        
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
        assertEquals("package:com.antos.gaming", intent.dataString)
    }
    
    @Test
    fun `getWriteSettingsPermissionIntent generates correct intent`() {
        val mockContext = mock<Context>()
        whenever(mockContext.packageName).thenReturn("com.antos.gaming")

        val intent = PermissionManager.getWriteSettingsPermissionIntent(mockContext)
        
        assertEquals(Settings.ACTION_MANAGE_WRITE_SETTINGS, intent.action)
        assertEquals("package:com.antos.gaming", intent.dataString)
    }
}
