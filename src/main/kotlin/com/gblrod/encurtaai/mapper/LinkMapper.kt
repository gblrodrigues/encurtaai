package com.gblrod.encurtaai.mapper

import com.gblrod.encurtaai.dto.LinkResponseDto
import com.gblrod.encurtaai.dto.TrashLinkResponseDto
import com.gblrod.encurtaai.entity.Link

fun Link.toResponse(baseUrl: String): LinkResponseDto {
    return LinkResponseDto(
        id = id,
        originalUrl = originalUrl,
        shortCode = shortCode,
        shortUrl = "$baseUrl/$shortCode",
        accessCount = accessCount,
        lastAccessedAt = lastAccessedAt
    )
}

fun Link.toTrashResponse(baseUrl: String): TrashLinkResponseDto {
    return TrashLinkResponseDto(
        id = id,
        originalUrl = originalUrl,
        shortCode = shortCode,
        shortUrl = "$baseUrl/$shortCode",
        accessCount = accessCount,
        lastAccessedAt = lastAccessedAt,
        deletedAt = requireNotNull(value = deletedAt) {
            "Cannot map an active link to TrashLinkResponseDto."
        }
    )
}