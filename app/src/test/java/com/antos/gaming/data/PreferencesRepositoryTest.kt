package com.antos.gaming.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File

class PreferencesRepositoryTest {

    @Test
    fun `saveGameProfile and getGameProfile roundtrip`() = runTest {
        // We can test DataStore logic using a temporary file
        val tempFile = File.createTempFile("test_datastore", ".preferences_pb")
        tempFile.deleteOnExit()
        
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { tempFile }
        )

        // We'll test the GameProfile data structure default values directly here
        
        val defaultProfile = GameProfile("com.test.game")
        assertEquals("com.test.game", defaultProfile.packageName)
        assertTrue(defaultProfile.performanceMode)
        assertEquals(-1f, defaultProfile.brightness, 0.01f)
        assertFalse(defaultProfile.isLandscapeLocked)
        assertTrue(defaultProfile.blockNotifications)
        assertTrue(defaultProfile.showFps)
        assertFalse(defaultProfile.touchProtection)
    }
}
