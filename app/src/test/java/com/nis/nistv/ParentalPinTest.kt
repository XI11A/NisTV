package com.nis.nistv

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.security.ParentalPinHelper
import com.nis.nistv.data.parental.ParentalControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalPinTest {

    @Test
    fun pinHashingAndVerification_worksReliably() {
        val pin = "1234"
        val salt = ParentalPinHelper.generateSalt()
        val hash = ParentalPinHelper.hashPin(pin, salt)

        assertTrue("Verification with correct PIN must succeed", ParentalPinHelper.verifyPin("1234", hash, salt))
        assertFalse("Verification with incorrect PIN must fail", ParentalPinHelper.verifyPin("9999", hash, salt))
        assertFalse("Empty PIN must fail", ParentalPinHelper.verifyPin("", hash, salt))
    }

    @Test
    fun lockoutCalculation_returnsRemainingTimeOrZero() {
        val now = 1_000_000L
        val lockoutUntil = now + 120_000L // 2 mins remaining

        val remaining = ParentalPinHelper.calculateLockoutRemainingMs(lockoutUntil, now)
        assertEquals(120_000L, remaining)

        val expired = ParentalPinHelper.calculateLockoutRemainingMs(lockoutUntil - 200_000L, now)
        assertEquals(0L, expired)
    }

    @Test
    fun onFailedAttempt_triggersLockoutAfterMaxAttempts() {
        val now = 2_000_000L

        // 1st to 4th attempts
        val (att1, lock1) = ParentalPinHelper.onFailedAttempt(0, now)
        assertEquals(1, att1)
        assertEquals(0L, lock1)

        val (att4, lock4) = ParentalPinHelper.onFailedAttempt(3, now)
        assertEquals(4, att4)
        assertEquals(0L, lock4)

        // 5th attempt triggers lockout
        val (att5, lock5) = ParentalPinHelper.onFailedAttempt(4, now)
        assertEquals(0, att5)
        assertEquals(now + ParentalPinHelper.LOCKOUT_DURATION_MS, lock5)
    }

    @Test
    fun channelVisibilityPredicate_respectsParentalLocks() {
        val normalChannel = ChannelEntity(
            id = "StarSports.in",
            name = "Star Sports",
            cleanName = "star sports",
            category = "Sports",
            isNsfw = false
        )
        val nsfwChannel = ChannelEntity(
            id = "AdultContent.in",
            name = "Adult Channel",
            cleanName = "adult channel",
            category = "General",
            isNsfw = true
        )
        val lockedCategoryChannel = ChannelEntity(
            id = "XxxLateNight.in",
            name = "Late Night",
            cleanName = "late night",
            category = "Adult",
            isNsfw = false
        )

        val lockedCategories = setOf("Adult", "XXX")

        // 1. Parental controls disabled -> all channels visible
        assertTrue(
            ParentalControlManager.isChannelVisible(
                normalChannel,
                isParentalEnabled = false,
                isUnlocked = false,
                lockedCategories = lockedCategories
            )
        )
        assertTrue(
            ParentalControlManager.isChannelVisible(
                nsfwChannel,
                isParentalEnabled = false,
                isUnlocked = false,
                lockedCategories = lockedCategories
            )
        )

        // 2. Parental controls active & locked -> restricted channels hidden in standard views
        assertTrue(
            ParentalControlManager.isChannelVisible(
                normalChannel,
                isParentalEnabled = true,
                isUnlocked = false,
                lockedCategories = lockedCategories,
                isFavoriteContext = false
            )
        )
        assertFalse(
            ParentalControlManager.isChannelVisible(
                nsfwChannel,
                isParentalEnabled = true,
                isUnlocked = false,
                lockedCategories = lockedCategories,
                isFavoriteContext = false
            )
        )
        assertFalse(
            ParentalControlManager.isChannelVisible(
                lockedCategoryChannel,
                isParentalEnabled = true,
                isUnlocked = false,
                lockedCategories = lockedCategories,
                isFavoriteContext = false
            )
        )

        // 3. Parental controls active & locked -> restricted channels RETAINED in favorites (with lock badge)
        assertTrue(
            ParentalControlManager.isChannelVisible(
                nsfwChannel,
                isParentalEnabled = true,
                isUnlocked = false,
                lockedCategories = lockedCategories,
                isFavoriteContext = true
            )
        )

        // 4. Temporarily unlocked -> all channels visible
        assertTrue(
            ParentalControlManager.isChannelVisible(
                nsfwChannel,
                isParentalEnabled = true,
                isUnlocked = true,
                lockedCategories = lockedCategories,
                isFavoriteContext = false
            )
        )
    }
}
