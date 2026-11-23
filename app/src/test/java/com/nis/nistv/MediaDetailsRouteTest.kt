package com.nis.nistv

import com.nis.nistv.core.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDetailsRouteTest {

    @Test
    fun createRoute_withoutOrigin_returnsCleanPath() {
        val route = Screen.MediaDetails.createRoute("movie", 550)
        assertEquals("details/movie/550", route)
    }

    @Test
    fun createRoute_withBlankOrigin_returnsCleanPath() {
        val route = Screen.MediaDetails.createRoute("tv", 101, "   ")
        assertEquals("details/tv/101", route)
    }

    @Test
    fun createRoute_withOrigin_returnsQueryParam() {
        val route = Screen.MediaDetails.createRoute("movie", 550, "home_trending")
        assertEquals("details/movie/550?origin=home_trending", route)
    }

    @Test
    fun createRoute_withSpecialCharacters_urlEncodesOrigin() {
        val route = Screen.MediaDetails.createRoute("tv", 202, "home row with spaces & symbols!")
        assertEquals("details/tv/202?origin=home%20row%20with%20spaces%20%26%20symbols%21", route)
    }
}
