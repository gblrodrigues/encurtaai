package com.gblrod.encurtaai.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Response containing the details of a deleted shortened URL.")
data class TrashLinkResponseDto(
    @field:Schema(
        description = "Unique identifier of the link.",
        example = "1"
    )

    val id: Long,

    @field:Schema(
        description = "Original destination URL.",
        example = "https://github.com/"
    )
    val originalUrl: String,

    @field:Schema(
        description = "Short code identifying the link.",
        example = "my-github"
    )
    val shortCode: String,

    @field:Schema(
        description = "Complete shortened URL.",
        example = "https://encurtaai-5q3b.onrender.com/my-github"
    )
    val shortUrl: String,

    @field:Schema(
        description = "Total number of times the shortened URL has been accessed.",
        example = "26"
    )
    val accessCount: Long,

    @field:Schema(
        description = "Timestamp of the most recent access.",
        example = "2026-11-10T18:57:43Z"
    )
    val lastAccessedAt: OffsetDateTime?,

    @field:Schema(
        description = "Date and time when the link was deleted.",
        example = "2026-11-16T10:00:00Z"
    )
    val deletedAt: OffsetDateTime
)