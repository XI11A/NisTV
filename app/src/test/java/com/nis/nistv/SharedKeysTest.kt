package com.nis.nistv

import com.nis.nistv.core.designsystem.transition.SharedKeys
import com.nis.nistv.core.designsystem.transition.SharedRole
import org.junit.Assert.*
import org.junit.Test

class SharedKeysTest {

    @Test
    fun roleUniqueness_differentRolesProduceDifferentKeys() {
        val containerKey = SharedKeys.of("movie", 550, SharedRole.CONTAINER, "home_trending")
        val imageKey = SharedKeys.of("movie", 550, SharedRole.IMAGE, "home_trending")
        val titleKey = SharedKeys.of("movie", 550, SharedRole.TITLE, "home_trending")

        assertNotEquals(containerKey, imageKey)
        assertNotEquals(imageKey, titleKey)
        assertNotEquals(containerKey, titleKey)

        assertEquals("movie_container_550_home_trending", containerKey)
        assertEquals("movie_image_550_home_trending", imageKey)
        assertEquals("movie_title_550_home_trending", titleKey)
    }

    @Test
    fun rowUniqueness_differentRowsProduceDifferentKeys() {
        val trendingKey = SharedKeys.of("movie", 550, SharedRole.CONTAINER, "home_trending")
        val topRatedKey = SharedKeys.of("movie", 550, SharedRole.CONTAINER, "home_top_rated")

        assertNotEquals(trendingKey, topRatedKey)
        assertEquals("movie_container_550_home_trending", trendingKey)
        assertEquals("movie_container_550_home_top_rated", topRatedKey)
    }

    @Test
    fun mediaTypeUniqueness_sameIdDifferentMediaType() {
        val movieKey = SharedKeys.of("movie", 100, SharedRole.IMAGE, "discover")
        val tvKey = SharedKeys.of("tv", 100, SharedRole.IMAGE, "discover")

        assertNotEquals(movieKey, tvKey)
        assertEquals("movie_image_100_discover", movieKey)
        assertEquals("tv_image_100_discover", tvKey)
    }

    @Test
    fun blankOrigin_fallsBackToDefault() {
        val blankKey = SharedKeys.of("movie", 42, SharedRole.CONTAINER, "   ")
        val emptyKey = SharedKeys.of("movie", 42, SharedRole.CONTAINER, "")

        assertEquals("movie_container_42_default", blankKey)
        assertEquals("movie_container_42_default", emptyKey)
    }
}
