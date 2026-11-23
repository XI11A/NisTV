package com.nis.nistv

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.feature.search.SearchUiState
import org.junit.Assert.*
import org.junit.Test

class SearchViewModelTest {

    @Test
    fun searchUiState_computesTotalCount_acrossAllMediaSources() {
        val channel1 = ChannelEntity(id = "ch_1", name = "BBC News", cleanName = "BBC News", category = "News")
        val channel2 = ChannelEntity(id = "ch_2", name = "CNN", cleanName = "CNN", category = "News")

        val movie1 = TmdbMediaItem(id = 101L, title = "Interstellar", overview = "", posterPath = null, backdropPath = null, releaseYear = "2014", voteAverage = 8.6, mediaType = TmdbMediaType.MOVIE)
        val tv1 = TmdbMediaItem(id = 201L, title = "Stranger Things", overview = "", posterPath = null, backdropPath = null, releaseYear = "2016", voteAverage = 8.6, mediaType = TmdbMediaType.TV)

        val state = SearchUiState(
            query = "news",
            channelResults = listOf(channel1, channel2),
            movieResults = listOf(movie1),
            tvResults = listOf(tv1),
            selectedFilterTab = "All",
            isSearching = true
        )

        assertEquals(4, state.totalCount)
        assertEquals(2, state.channelResults.size)
        assertEquals(1, state.movieResults.size)
        assertEquals(1, state.tvResults.size)
        assertTrue(state.isSearching)
    }

    @Test
    fun searchUiState_emptyWhenNoQuery() {
        val state = SearchUiState(query = "")
        assertFalse(state.isSearching)
        assertEquals(0, state.totalCount)
    }

    @Test
    fun searchUiState_rapidTypingQuerySequence_maintainsCorrectQuery() {
        val keystrokes = listOf("F", "Fi", "Fir", "Fire")
        var currentState = SearchUiState()
        for (stroke in keystrokes) {
            currentState = currentState.copy(query = stroke, isSearching = stroke.isNotBlank())
            assertEquals(stroke, currentState.query)
            assertTrue(currentState.isSearching)
        }
        assertEquals("Fire", currentState.query)
    }
}
