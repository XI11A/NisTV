package com.nis.nistv.feature.player.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nis.nistv.R
import kotlinx.coroutines.delay

/**
 * 5-second cinematic NisTV branding intro pre-roll component for offline and online video players.
 *
 * Choreographed to match the NisTV animated splash sequence with zero network overhead.
 * Plays the full 5-second intro without skip button before target content starts.
 */
@Composable
fun PlayerBrandingIntro(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasFinished by remember { mutableStateOf(false) }

    val finishOnce = remember {
        {
            if (!hasFinished) {
                hasFinished = true
                onFinish()
            }
        }
    }

    val introPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.nistv_branding}")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(introPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    finishOnce()
                }
            }
        }
        introPlayer.addListener(listener)

        onDispose {
            introPlayer.removeListener(listener)
            introPlayer.stop()
            introPlayer.release()
        }
    }

    LaunchedEffect(introPlayer) {
        while (!hasFinished) {
            val cur = introPlayer.currentPosition
            if (cur >= 5000L) {
                finishOnce()
                break
            }
            delay(100L)
        }
    }

    AnimatedVisibility(
        visible = !hasFinished,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = introPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
