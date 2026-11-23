package com.antos.gaming.ui.sidebar

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antos.gaming.FloatingAppLauncher

private val AntosBg = Color(0xF9080712)
private val Purple = Color(0xFF9B35FF)
private val BrightPurple = Color(0xFFC66AFF)
private val TextWhite = Color(0xFFF4ECFF)
private val Muted = Color(0xFF9387A5)

@Composable
fun GamingSidebar() {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(250.dp)
            .background(
                AntosBg,
                RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                Purple,
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {

        Text(
            text = "ANTOS",
            color = BrightPurple,
            fontSize = 23.sp
        )

        Text(
            text = "GAMING CORE",
            color = Muted,
            fontSize = 9.sp,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "QUICK APPS",
            color = Purple,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(8.dp))

        FloatingAppButton(
            icon = Icons.Default.Chat,
            text = "DISCORD"
        ) {
            FloatingAppLauncher.launch(
                context,
                FloatingAppLauncher.DISCORD
            )
        }

        FloatingAppButton(
            icon = Icons.Default.Language,
            text = "BRAVE"
        ) {
            FloatingAppLauncher.launch(
                context,
                FloatingAppLauncher.BRAVE
            )
        }

        FloatingAppButton(
            icon = Icons.Default.CameraAlt,
            text = "INSTAGRAM"
        ) {
            FloatingAppLauncher.launch(
                context,
                FloatingAppLauncher.INSTAGRAM
            )
        }

        Spacer(Modifier.height(15.dp))

        Text(
            text = "GAME TOOLS",
            color = Purple,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(8.dp))

        FloatingAppButton(
            icon = Icons.Default.Videocam,
            text = "SCREEN RECORD"
        ) {
            // Add MediaProjection recorder here
        }

        FloatingAppButton(
            icon = Icons.Default.NotificationsOff,
            text = "DND"
        ) {
            // Add Notification Policy Access here
        }

        FloatingAppButton(
            icon = Icons.Default.Speed,
            text = "FPS"
        ) {
            // Open FPS overlay
        }

        Spacer(Modifier.height(8.dp))

        FloatingAppButton(
            icon = Icons.Default.Close,
            text = "CLOSE GAME MODE"
        ) {
            val stopIntent = android.content.Intent(context, com.antos.gaming.service.OverlayService::class.java).apply {
                action = com.antos.gaming.service.OverlayService.ACTION_STOP_SERVICE
            }
            context.startService(stopIntent)
        }
    }
}

@Composable
fun FloatingAppButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable {
                onClick()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = BrightPurple,
            modifier = Modifier.size(25.dp)
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = text,
            color = TextWhite,
            fontSize = 12.sp
        )
    }
}
