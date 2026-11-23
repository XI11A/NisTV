package com.nis.nistv

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.security.KidsContentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsContentFilterTest {

    private fun createChannel(
        id: String,
        name: String,
        category: String,
        isNsfw: Boolean = false
    ) = ChannelEntity(
        id = id,
        name = name,
        cleanName = name.lowercase(),
        logoUrl = "",
        category = category,
        language = "Hindi",
        country = "IN",
        isNsfw = isNsfw
    )

    @Test
    fun allowlistedCategories_containRequiredFamilies() {
        val expected = setOf("Animation", "Education", "Family", "Kids")
        assertEquals(expected, KidsContentFilter.ALLOWED_CHANNEL_CATEGORIES)
    }

    @Test
    fun allowlistedTmdbGenres_containAnimationFamilyKids() {
        val expected = setOf(16, 10751, 10762)
        assertEquals(expected, KidsContentFilter.ALLOWED_TMDB_GENRE_IDS)
    }

    @Test
    fun isChannelKidFriendly_permitsAllowedCategoriesOnly() {
        val animation = createChannel("ch_1", "Pogo", "Animation")
        val education = createChannel("ch_2", "Discovery Kids", "Education")
        val family = createChannel("ch_3", "Disney", "Family")
        val kids = createChannel("ch_4", "Cartoon Network", "Kids")
        val adultNews = createChannel("ch_5", "CNN", "News")
        val nsfw = createChannel("ch_6", "Adult Animation", "Animation", isNsfw = true)

        assertTrue("Animation must be allowed", KidsContentFilter.isChannelAllowedForKids(animation))
        assertTrue("Education must be allowed", KidsContentFilter.isChannelAllowedForKids(education))
        assertTrue("Family must be allowed", KidsContentFilter.isChannelAllowedForKids(family))
        assertTrue("Kids must be allowed", KidsContentFilter.isChannelAllowedForKids(kids))

        assertFalse("News must not be allowed for Kids", KidsContentFilter.isChannelAllowedForKids(adultNews))
        assertFalse("NSFW channels must never be allowed for Kids", KidsContentFilter.isChannelAllowedForKids(nsfw))
    }

    @Test
    fun isGenreKidFriendly_matchesExpectedIds() {
        assertTrue("16 (Animation) is kid friendly", KidsContentFilter.isGenreAllowedForKids(16))
        assertTrue("10751 (Family) is kid friendly", KidsContentFilter.isGenreAllowedForKids(10751))
        assertTrue("10762 (Kids) is kid friendly", KidsContentFilter.isGenreAllowedForKids(10762))

        assertFalse("28 (Action) is not kid friendly", KidsContentFilter.isGenreAllowedForKids(28))
        assertFalse("27 (Horror) is not kid friendly", KidsContentFilter.isGenreAllowedForKids(27))
        assertFalse("18 (Drama) is not kid friendly", KidsContentFilter.isGenreAllowedForKids(18))
    }

    @Test
    fun filterChannels_withKidsProfile_filtersOutNonKidsChannels() {
        val channels = listOf(
            createChannel("ch_1", "Pogo", "Animation"),
            createChannel("ch_2", "BBC News", "News"),
            createChannel("ch_3", "Cartoon Network", "Kids"),
            createChannel("ch_4", "Sports 18", "Sports")
        )

        val filtered = KidsContentFilter.filterChannels(
            channels = channels,
            isKidsProfile = true,
            isParentalEnabled = false,
            isUnlocked = false,
            lockedCategories = emptySet()
        )

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.name == "Pogo" })
        assertTrue(filtered.any { it.name == "Cartoon Network" })
        assertFalse(filtered.any { it.name == "BBC News" })
        assertFalse(filtered.any { it.name == "Sports 18" })
    }

    @Test
    fun filterChannels_withNormalProfile_preservesAllChannels() {
        val channels = listOf(
            createChannel("ch_1", "Pogo", "Animation"),
            createChannel("ch_2", "BBC News", "News"),
            createChannel("ch_3", "Cartoon Network", "Kids")
        )

        val filtered = KidsContentFilter.filterChannels(
            channels = channels,
            isKidsProfile = false,
            isParentalEnabled = false,
            isUnlocked = false,
            lockedCategories = emptySet()
        )

        assertEquals(3, filtered.size)
    }
}
