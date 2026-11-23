package com.antos.gaming.monitoring

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MonitorTest {

    @Test
    fun `BatteryMonitor parses level and scale correctly`() {
        val mockContext = mock<Context>()
        val monitor = BatteryMonitor(mockContext)

        // Reflection to invoke the private BroadcastReceiver for testing logic without Robolectric
        val receiverField = BatteryMonitor::class.java.getDeclaredField("receiver")
        receiverField.isAccessible = true
        val receiver = receiverField.get(monitor) as android.content.BroadcastReceiver

        val mockIntent = mock<Intent>()
        whenever(mockIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(42)
        whenever(mockIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)

        receiver.onReceive(mockContext, mockIntent)

        assertEquals(42, monitor.batteryLevel.value)
    }

    @Test
    fun `TemperatureMonitor parses battery temperature correctly`() {
        val mockContext = mock<Context>()
        val monitor = TemperatureMonitor(mockContext)

        val receiverField = TemperatureMonitor::class.java.getDeclaredField("receiver")
        receiverField.isAccessible = true
        val receiver = receiverField.get(monitor) as android.content.BroadcastReceiver

        val mockIntent = mock<Intent>()
        // Battery temperature is returned in tenths of a degree Celsius
        whenever(mockIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)).thenReturn(385)

        receiver.onReceive(mockContext, mockIntent)

        assertEquals(38.5f, monitor.temperature.value, 0.01f)
    }
}
