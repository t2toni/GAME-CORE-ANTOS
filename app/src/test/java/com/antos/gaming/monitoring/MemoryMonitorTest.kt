package com.antos.gaming.monitoring

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryMonitorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `MemoryMonitor calculates RAM in GB correctly`() = testScope.runTest {
        val mockContext = mock<Context>()
        val mockActivityManager = mock<ActivityManager>()

        whenever(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mockActivityManager)

        // Mock the getMemoryInfo method to populate our MemoryInfo object
        doAnswer { invocation ->
            val outInfo = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            // 5 GB available, 12 GB total
            outInfo.availMem = (5L * 1024 * 1024 * 1024)
            outInfo.totalMem = (12L * 1024 * 1024 * 1024)
            null
        }.whenever(mockActivityManager).getMemoryInfo(any())

        val monitor = MemoryMonitor(mockContext)
        monitor.start(this)
        
        // Advance time to allow the coroutine to tick
        advanceTimeBy(100)

        assertEquals(5.0f, monitor.availableMemoryGb.value, 0.01f)
        assertEquals(12.0f, monitor.totalMemoryGb.value, 0.01f)
        
        monitor.stop()
    }
}
