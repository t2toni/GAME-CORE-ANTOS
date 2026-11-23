package com.antos.gaming.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val Purple = Color(0xFF9B35FF)
private val BrightPurple = Color(0xFFC66AFF)

@Composable
fun EdgeHandle(
    onSwipeRight: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .fillMaxHeight()
            .background(
                BrightPurple.copy(alpha = 0.3f),
                RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount > 50) {
                        onSwipeRight()
                    }
                }
            }
    )
}
