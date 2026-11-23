package com.nis.nistv.feature.player.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun PlayerGestureDetector(
    onToggleControls: () -> Unit,
    onAdjustBrightness: (delta: Float) -> Unit,
    onAdjustVolume: (delta: Float) -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var isDraggingLeft by remember { mutableStateOf(false) }
    var totalDragX by remember { mutableStateOf(0f) }
    var totalDragY by remember { mutableStateOf(0f) }
    var hasTriggeredHorizontalSwipe by remember { mutableStateOf(false) }
    var isHorizontalGesture by remember { mutableStateOf<Boolean?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isLocked) {
                if (isLocked) {
                    detectTapGestures(
                        onTap = { onToggleControls() }
                    )
                } else {
                    detectTapGestures(
                        onTap = { onToggleControls() },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                onDoubleTapLeft()
                            } else {
                                onDoubleTapRight()
                            }
                        }
                    )
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDraggingLeft = offset.x < size.width / 2
                            totalDragX = 0f
                            totalDragY = 0f
                            hasTriggeredHorizontalSwipe = false
                            isHorizontalGesture = null
                        },
                        onDragEnd = {
                            isHorizontalGesture = null
                            hasTriggeredHorizontalSwipe = false
                        },
                        onDragCancel = {
                            isHorizontalGesture = null
                            hasTriggeredHorizontalSwipe = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (isHorizontalGesture == null) {
                                if (kotlin.math.abs(totalDragX) > 40f && kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY) * 1.5f && (onSwipeLeft != null || onSwipeRight != null)) {
                                    isHorizontalGesture = true
                                } else if (kotlin.math.abs(totalDragY) > 30f) {
                                    isHorizontalGesture = false
                                }
                            }

                            if (isHorizontalGesture == true) {
                                val threshold = size.width * 0.18f
                                if (!hasTriggeredHorizontalSwipe) {
                                    if (totalDragX < -threshold) {
                                        hasTriggeredHorizontalSwipe = true
                                        onSwipeLeft?.invoke()
                                    } else if (totalDragX > threshold) {
                                        hasTriggeredHorizontalSwipe = true
                                        onSwipeRight?.invoke()
                                    }
                                }
                            } else {
                                // Vertical brightness/volume adjustment
                                val delta = -dragAmount.y / size.height
                                if (isDraggingLeft) {
                                    onAdjustBrightness(delta * 1.5f)
                                } else {
                                    onAdjustVolume(delta * 1.5f)
                                }
                            }
                        }
                    )
                }
            }
    ) {
        content()
    }
}
