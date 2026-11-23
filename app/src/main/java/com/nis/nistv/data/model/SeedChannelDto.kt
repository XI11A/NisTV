package com.nis.nistv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SeedChannelDto(
    val id: String,
    val name: String,
    val cleanName: String = "",
    val altNames: String = "",
    val country: String = "IN",
    val network: String = "",
    val website: String = "",
    val language: String = "Hindi",
    val category: String = "Entertainment",
    val logoUrl: String = "",
    val resolution: String = "HD",
    val isNsfw: Boolean = false,
    val labels: String = "",
    val streamUrl: String
)
