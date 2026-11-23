package com.nis.nistv.playback.multiview

data class TileDimension(
    val widthDp: Float,
    val heightDp: Float,
    val isStackedVertically: Boolean
)

object MultiViewLayoutHelper {

    /**
     * Calculates the layout dimensions for 2 video tiles:
     * - Portrait: 2 stacked 16:9 tiles filling screen width
     * - Landscape: 2 side-by-side 16:9 tiles filling half the screen width each
     */
    fun calculateTileDimensions(
        screenWidthDp: Float,
        screenHeightDp: Float,
        isLandscape: Boolean
    ): TileDimension {
        return if (isLandscape) {
            val tileWidth = screenWidthDp / 2f
            val naturalHeight = tileWidth * (9f / 16f)
            val tileHeight = minOf(naturalHeight, screenHeightDp)
            TileDimension(widthDp = tileWidth, heightDp = tileHeight, isStackedVertically = false)
        } else {
            val tileWidth = screenWidthDp
            val naturalHeight = tileWidth * (9f / 16f)
            val maxHeightPerTile = screenHeightDp / 2f
            val tileHeight = minOf(naturalHeight, maxHeightPerTile)
            TileDimension(widthDp = tileWidth, heightDp = tileHeight, isStackedVertically = true)
        }
    }
}
