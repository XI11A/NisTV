package com.nis.nistv

import com.nis.nistv.data.tmdb.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TmdbModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun movieDto_mapsToMediaItem_correctly() {
        val movie = TmdbMovieDto(
            id = 101L,
            title = "Inception",
            overview = "A thief who steals corporate secrets...",
            posterPath = "/inception_poster.jpg",
            backdropPath = "/inception_backdrop.jpg",
            releaseDate = "2010-07-16",
            voteAverage = 8.36,
            voteCount = 35000,
            genreIds = listOf(28, 878, 12)
        )

        val item = movie.toMediaItem()

        assertEquals(101L, item.id)
        assertEquals("Inception", item.title)
        assertEquals("2010", item.releaseYear)
        assertEquals(TmdbMediaType.MOVIE, item.mediaType)
        assertEquals("https://image.tmdb.org/t/p/w342/inception_poster.jpg", item.fullPosterUrl)
        assertEquals("https://image.tmdb.org/t/p/w780/inception_backdrop.jpg", item.fullBackdropUrl)
        assertTrue(item.formattedRating.contains("8.4"))
    }

    @Test
    fun tvDto_mapsToMediaItem_correctly() {
        val tv = TmdbTvDto(
            id = 202L,
            name = "Breaking Bad",
            overview = "A chemistry teacher diagnosed with inoperable lung cancer...",
            posterPath = "/bb_poster.jpg",
            backdropPath = "/bb_backdrop.jpg",
            firstAirDate = "2008-01-20",
            voteAverage = 8.89,
            genreIds = listOf(18, 80)
        )

        val item = tv.toMediaItem()

        assertEquals(202L, item.id)
        assertEquals("Breaking Bad", item.title)
        assertEquals("2008", item.releaseYear)
        assertEquals(TmdbMediaType.TV, item.mediaType)
        assertTrue(item.formattedRating.contains("8.9"))
    }

    @Test
    fun tmdbVideoDto_identifiesYouTubeTrailers() {
        val trailer = TmdbVideoDto(
            id = "vid_1",
            key = "dQw4w9WgXcQ",
            name = "Official Trailer",
            site = "YouTube",
            type = "Trailer",
            official = true
        )

        assertTrue(trailer.isYouTubeTrailer)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", trailer.youTubeUrl)

        val featurette = TmdbVideoDto(
            id = "vid_2",
            key = "abc123xyz",
            name = "Behind The Scenes",
            site = "Vimeo",
            type = "Featurette",
            official = false
        )

        assertFalse(featurette.isYouTubeTrailer)
    }

    @Test
    fun pagedResponse_deserializesCorrectly() {
        val rawJson = """
            {
                "page": 1,
                "results": [
                    {
                        "id": 550,
                        "title": "Fight Club",
                        "overview": "An insomniac office worker...",
                        "poster_path": "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                        "release_date": "1999-10-15",
                        "vote_average": 8.433
                    }
                ],
                "total_pages": 10,
                "total_results": 200
            }
        """.trimIndent()

        val parsed = json.decodeFromString<TmdbPagedResponse<TmdbMovieDto>>(rawJson)

        assertEquals(1, parsed.page)
        assertEquals(10, parsed.totalPages)
        assertEquals(200, parsed.totalResults)
        assertEquals(1, parsed.results.size)
        assertEquals(550L, parsed.results[0].id)
        assertEquals("Fight Club", parsed.results[0].title)
        assertEquals("1999", parsed.results[0].toMediaItem().releaseYear)
    }
}
