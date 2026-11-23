package com.nis.nistv.feature.player.components

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class TargetOrientation {
    PORTRAIT,
    LANDSCAPE,
    REVERSE_LANDSCAPE,
    UNKNOWN
}

/**
 * Maps hardware orientation sensor angle (0..359 degrees) to target orientation,
 * incorporating hysteresis buffers to eliminate jitter.
 */
fun resolveTargetOrientation(
    angle: Int,
    isNaturalLandscape: Boolean = false
): TargetOrientation {
    if (angle == OrientationEventListener.ORIENTATION_UNKNOWN || angle < 0) {
        return TargetOrientation.UNKNOWN
    }

    return if (!isNaturalLandscape) {
        // Standard phone: Natural orientation is Portrait
        when (angle) {
            in 325..360, in 0..35 -> TargetOrientation.PORTRAIT
            in 55..125 -> TargetOrientation.REVERSE_LANDSCAPE
            in 235..305 -> TargetOrientation.LANDSCAPE
            else -> TargetOrientation.UNKNOWN
        }
    } else {
        // Tablet: Natural orientation is Landscape
        when (angle) {
            in 325..360, in 0..35 -> TargetOrientation.LANDSCAPE
            in 145..215 -> TargetOrientation.REVERSE_LANDSCAPE
            in 55..125 -> TargetOrientation.PORTRAIT
            else -> TargetOrientation.UNKNOWN
        }
    }
}

/**
 * Checks whether the physical device has a natural landscape orientation (common on tablets).
 */
fun isNaturalOrientationLandscape(context: Context): Boolean {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
    val rotation = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: windowManager.defaultDisplay.rotation
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
    } catch (_: Exception) {
        Surface.ROTATION_0
    }
    val config = context.resources.configuration
    val isCurrentLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    return ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && isCurrentLandscape) ||
           ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && !isCurrentLandscape)
}

/**
 * Checks if auto-rotate is enabled in Android system settings.
 */
fun isAutoRotateEnabled(context: Context): Boolean {
    return try {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0
        ) == 1
    } catch (_: Exception) {
        false
    }
}

/**
 * YouTube-style orientation controller for video playback:
 * 1. Supports fluid 180° rotation between Landscape and Reverse Landscape while in fullscreen,
 *    even when system auto-rotate is disabled.
 * 2. If system auto-rotate is ON, physically turning the phone upright exits fullscreen automatically.
 * 3. Tapping Exit Fullscreen while still holding the phone in landscape locks portrait until
 *    the device is physically rotated upright, preventing immediate snap-backs.
 * 4. Respects in-player lock controls (isLocked) and Picture-in-Picture (isInPip).
 * 5. Safely restores SCREEN_ORIENTATION_UNSPECIFIED upon departure.
 */
class PlayerOrientationController(
    private val context: Context,
    private val activity: Activity?,
) {
    var isLandscape: Boolean = false
        private set
    var isLocked: Boolean = false
        private set
    var isInPip: Boolean = false
        private set

    private var userLockedToPortrait: Boolean = false
    private var lastKnownDeviceAngle: Int = -1
    private val isNaturalLandscape = isNaturalOrientationLandscape(context)

    private val orientationEventListener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            lastKnownDeviceAngle = orientation

            // If user has locked controls or player is in PiP, ignore rotation events
            if (isLocked || isInPip) return

            val isAutoRotateOn = isAutoRotateEnabled(context)
            val target = resolveTargetOrientation(orientation, isNaturalLandscape)
            if (target == TargetOrientation.UNKNOWN) return

            when (target) {
                TargetOrientation.PORTRAIT -> {
                    if (userLockedToPortrait) {
                        // User physically returned the phone to upright portrait
                        userLockedToPortrait = false
                        setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    } else if (isAutoRotateOn && isLandscape) {
                        // In fullscreen with auto-rotate ON, physically turning upright exits fullscreen
                        setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    }
                }
                TargetOrientation.LANDSCAPE -> {
                    if (!userLockedToPortrait) {
                        // 180° flip from reverse landscape or auto-rotate from portrait
                        if (isLandscape || isAutoRotateOn) {
                            setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                    }
                }
                TargetOrientation.REVERSE_LANDSCAPE -> {
                    if (!userLockedToPortrait) {
                        // 180° flip from normal landscape or auto-rotate from portrait
                        if (isLandscape || isAutoRotateOn) {
                            setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                    }
                }
                TargetOrientation.UNKNOWN -> {}
            }
        }
    }

    fun updateState(isLandscape: Boolean, isLocked: Boolean, isInPip: Boolean) {
        this.isLandscape = isLandscape
        this.isLocked = isLocked
        this.isInPip = isInPip
    }

    /**
     * Enters fullscreen landscape mode. Respects current physical tilt if already horizontal.
     */
    fun enterFullscreen() {
        userLockedToPortrait = false
        val target = resolveTargetOrientation(lastKnownDeviceAngle, isNaturalLandscape)
        when (target) {
            TargetOrientation.REVERSE_LANDSCAPE -> {
                setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
            }
            TargetOrientation.LANDSCAPE -> {
                setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }
            else -> {
                setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            }
        }
    }

    /**
     * Exits fullscreen mode. If device is still physically held in landscape, locks to portrait
     * until physically turned upright to avoid immediate snap-back.
     */
    fun exitFullscreen() {
        val target = resolveTargetOrientation(lastKnownDeviceAngle, isNaturalLandscape)
        if (target == TargetOrientation.LANDSCAPE || target == TargetOrientation.REVERSE_LANDSCAPE) {
            userLockedToPortrait = true
            setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        } else {
            userLockedToPortrait = false
            setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        }
    }

    /**
     * Unconditionally resets orientation to system default.
     */
    fun resetOrientation() {
        userLockedToPortrait = false
        setOrientationSafely(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    fun enable() {
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
    }

    fun disable() {
        orientationEventListener.disable()
    }

    private fun setOrientationSafely(targetOrientation: Int) {
        val act = activity ?: return
        if (act.requestedOrientation != targetOrientation) {
            act.requestedOrientation = targetOrientation
        }
    }
}

/**
 * Remember and observe a PlayerOrientationController within Compose lifecycle.
 */
@Composable
fun rememberPlayerOrientationController(
    activity: Activity?,
    isLandscape: Boolean,
    isLocked: Boolean = false,
    isInPip: Boolean = false,
): PlayerOrientationController {
    val context = LocalContext.current
    val controller = remember(context, activity) {
        PlayerOrientationController(context, activity)
    }

    controller.updateState(isLandscape = isLandscape, isLocked = isLocked, isInPip = isInPip)

    DisposableEffect(controller, isInPip) {
        if (!isInPip) {
            controller.enable()
        } else {
            controller.disable()
        }
        onDispose {
            controller.disable()
            controller.resetOrientation()
        }
    }

    return controller
}
