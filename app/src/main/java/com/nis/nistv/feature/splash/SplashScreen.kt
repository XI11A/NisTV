package com.nis.nistv.feature.splash

import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nis.nistv.NisTvApplication
import kotlinx.coroutines.flow.firstOrNull
import com.nis.nistv.R
import com.nis.nistv.core.designsystem.component.NisAnimatedSvgLogo
import com.nis.nistv.core.designsystem.component.rememberReducedMotion
import com.nis.nistv.core.designsystem.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** System and Compose splash background color constant (#0B0D12) */
val SplashBackgroundColor = NisBackground

/**
 * Dedicated Splash Screen for NisTV.
 *
 * Runs as its own destination in the navigation graph (Screen.Splash).
 *
 * Key Architectural Refinements:
 * 1. Zero-color-jump handoff: Initial frame uses solid [SplashBackgroundColor] matching the system theme.
 * 2. Deterministic timeline: Background settle, logo build, and staggered typography entrance.
 * 3. Boot-readiness exit orchestration: Waits for background initialization without redundant seed calls.
 * 4. Conditional loading feedback: Progress spinner is displayed only if boot is delayed >400ms after build.
 * 5. Reduced-motion compliance: Bypasses build animation with immediate logo presentation and rapid exit.
 * 6. Sonic brand identity: Plays synchronized brand mnemonic sound via ExoPlayer with MediaPlayer fallback on the media audio stream.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as? NisTvApplication
    val exitProgress = remember { Animatable(0f) }
    val screenTime = remember { Animatable(0f) }
    var showHint by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()

    // Synchronized brand sonic mnemonic playback on the media stream (STREAM_MUSIC)
    DisposableEffect(Unit) {
        val appContext = context.applicationContext
        val audioManager = appContext.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager

        var exoPlayer: ExoPlayer? = null
        var mediaPlayer: MediaPlayer? = null
        var lifecycleObserver: LifecycleEventObserver? = null

        val soundEnabled = true
        val ringerMode = audioManager?.ringerMode ?: android.media.AudioManager.RINGER_MODE_NORMAL
        val inCall = audioManager?.let {
            it.mode == android.media.AudioManager.MODE_IN_CALL || it.mode == android.media.AudioManager.MODE_IN_COMMUNICATION
        } ?: false
        val isMusicActive = audioManager?.isMusicActive ?: false

        if (SplashTimeline.shouldPlayStartupSound(soundEnabled, ringerMode, inCall, isMusicActive)) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    try {
                        exoPlayer?.stop()
                        exoPlayer?.release()
                    } catch (_: Throwable) {}
                    try {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.stop()
                        }
                        mediaPlayer?.release()
                    } catch (_: Throwable) {}
                    exoPlayer = null
                    mediaPlayer = null
                }
            }
            lifecycleObserver = observer
            lifecycleOwner.lifecycle.addObserver(observer)

            try {
                val player = ExoPlayer.Builder(appContext)
                    .setAudioAttributes(
                        Media3AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        /* handleAudioFocus = */ false
                    )
                    .build()

                val uri = Uri.parse("android.resource://${appContext.packageName}/${R.raw.nistv_splash}")
                player.setMediaItem(MediaItem.fromUri(uri))
                player.volume = 1.0f
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            try {
                                lifecycleObserver?.let { lifecycleOwner.lifecycle.removeObserver(it) }
                                player.release()
                            } catch (_: Throwable) {}
                            if (exoPlayer === player) {
                                exoPlayer = null
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        try {
                            lifecycleObserver?.let { lifecycleOwner.lifecycle.removeObserver(it) }
                            player.release()
                        } catch (_: Throwable) {}
                        if (exoPlayer === player) {
                            exoPlayer = null
                        }
                    }
                })
                player.prepare()
                player.playWhenReady = true
                exoPlayer = player
            } catch (_: Throwable) {
                // Resilient fallback to standard Android MediaPlayer
                try {
                    val mp = MediaPlayer.create(appContext, R.raw.nistv_splash)
                    mp?.apply {
                        setVolume(1.0f, 1.0f)
                        setOnCompletionListener { completed ->
                            try {
                                lifecycleObserver?.let { lifecycleOwner.lifecycle.removeObserver(it) }
                                completed.release()
                            } catch (_: Throwable) {}
                            if (mediaPlayer === completed) {
                                mediaPlayer = null
                            }
                        }
                        start()
                    }
                    mediaPlayer = mp
                } catch (_: Throwable) {
                    mediaPlayer = null
                }
            }
        }

        onDispose {
            // Note: We deliberately allow the 1.9s brand mnemonic to play to natural completion and self-release on STATE_ENDED.
            // This prevents clipping the tail reverb when navigating from Splash to Home.
            // If the user leaves/minimizes the app, the Lifecycle observer handles ON_STOP release.
        }
    }

    LaunchedEffect(reducedMotion) {
        val start = SystemClock.elapsedRealtime()

        if (reducedMotion) {
            screenTime.snapTo(SplashTimeline.BUILD_END_MS.toFloat())
        } else {
            launch {
                screenTime.animateTo(
                    targetValue = SplashTimeline.BUILD_END_MS.toFloat(),
                    animationSpec = tween(SplashTimeline.BUILD_END_MS, easing = LinearEasing)
                )
            }
        }

        // Await single boot-readiness signal from AppContainer with 4s hard timeout
        val bootSignal = app?.container?.bootReady
        val readyDeferred = async {
            withTimeoutOrNull(SplashTimeline.HARD_TIMEOUT_MS) {
                bootSignal?.await()
            }
        }

        // Check if boot is still pending 400ms after logo build finishes
        if (!reducedMotion) {
            delay(SplashTimeline.BUILD_END_MS + 400L)
            if (!readyDeferred.isCompleted) {
                showHint = true
            }
        }

        readyDeferred.await()

        val elapsed = SystemClock.elapsedRealtime() - start
        val remaining = SplashTimeline.computeRemainingWait(
            elapsedMs = elapsed,
            isBootReady = true,
            minDisplayMs = if (reducedMotion) 0L else SplashTimeline.MIN_DISPLAY_MS,
            hardTimeoutMs = SplashTimeline.HARD_TIMEOUT_MS
        )
        if (remaining > 0L) {
            delay(remaining)
        }

        // Play exit transition (logo scale 1.0 -> 1.06, alpha 1.0 -> 0.0)
        exitProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (reducedMotion) 150 else SplashTimeline.EXIT_MS,
                easing = FastOutSlowInEasing
            )
        )
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Base background is exactly #0B0D12 (matches system splash)
                drawRect(SplashBackgroundColor)

                // Subtle top navy glow settles in between 0 and 350 ms
                val bgProgress = SplashTimeline.progress(
                    screenTime.value,
                    SplashTimeline.Background,
                    FastOutSlowInEasing
                )
                if (bgProgress > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NisSurface,
                                SplashBackgroundColor,
                                SplashBackgroundColor
                            )
                        ),
                        alpha = bgProgress
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    val exit = exitProgress.value
                    alpha = (1f - exit).coerceIn(0f, 1f)
                    scaleX = 1f + 0.06f * exit
                    scaleY = 1f + 0.06f * exit
                }
        ) {
            // Choreographed Animated Vector Logo
            NisAnimatedSvgLogo(
                logoSize = 124.dp,
                reducedMotion = reducedMotion
            )

            // Staggered Typography
            val wordmarkProgress = SplashTimeline.progress(
                screenTime.value,
                SplashTimeline.Wordmark,
                SplashTimeline.EaseOutQuint
            )
            val taglineProgress = SplashTimeline.progress(
                screenTime.value,
                SplashTimeline.Tagline,
                SplashTimeline.EaseOutQuint
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NisTV",
                    color = NisTextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = wordmarkProgress
                        translationY = (1f - wordmarkProgress) * 10.dp.toPx()
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "NEXT-GEN ENTERTAINMENT & STREAMING",
                    color = NisAccentCyan,
                    style = NisTheme.textExtras.tagline,
                    modifier = Modifier.graphicsLayer {
                        alpha = taglineProgress
                        translationY = (1f - taglineProgress) * 10.dp.toPx()
                    }
                )
            }

            // Conditional Loading Feedback (Only visible if cold boot takes longer than expected)
            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    CircularProgressIndicator(
                        color = NisAccentPrimary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.4.dp
                    )

                    Text(
                        text = "Preparing entertainment experience...",
                        color = NisTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
