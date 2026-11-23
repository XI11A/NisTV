package com.nis.nistv

import android.media.AudioManager
import com.nis.nistv.feature.splash.SplashTimeline
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSoundTest {

    @Test
    fun shouldPlayStartupSound_playsWhenConditionsNormalAndEnabled() {
        val play = SplashTimeline.shouldPlayStartupSound(
            settingEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            inCall = false,
            musicActive = false
        )
        assertTrue("Sound should play when setting is enabled and device is normal", play)
    }

    @Test
    fun shouldPlayStartupSound_doesNotPlayWhenDisabledInSettings() {
        val play = SplashTimeline.shouldPlayStartupSound(
            settingEnabled = false,
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            inCall = false,
            musicActive = false
        )
        assertFalse("Sound must not play when disabled in user settings", play)
    }

    @Test
    fun shouldPlayStartupSound_playsEvenWhenRingerSilentOrVibrate() {
        val playSilent = SplashTimeline.shouldPlayStartupSound(
            settingEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_SILENT,
            inCall = false,
            musicActive = false
        )
        assertTrue("Media sound should play even in silent mode", playSilent)

        val playVibrate = SplashTimeline.shouldPlayStartupSound(
            settingEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_VIBRATE,
            inCall = false,
            musicActive = false
        )
        assertTrue("Media sound should play even in vibrate mode", playVibrate)
    }

    @Test
    fun shouldPlayStartupSound_doesNotPlayDuringPhoneCall() {
        val playInCall = SplashTimeline.shouldPlayStartupSound(
            settingEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            inCall = true,
            musicActive = false
        )
        assertFalse("Sound must not play when user is on a call", playInCall)
    }

    @Test
    fun shouldPlayStartupSound_playsWhenOtherAudioIsActive() {
        val playWithMusic = SplashTimeline.shouldPlayStartupSound(
            settingEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            inCall = false,
            musicActive = true
        )
        assertTrue("Sound should play even when other media is active", playWithMusic)
    }

    @Test
    fun testSplashRawAssetExists() {
        val rawAudio = java.io.File("src/main/res/raw/nistv_splash.mp3")
        assertTrue("nistv_splash.mp3 must exist in res/raw", rawAudio.exists())
        assertTrue("nistv_splash.mp3 must be non-empty", rawAudio.length() > 10_000)
    }
}
