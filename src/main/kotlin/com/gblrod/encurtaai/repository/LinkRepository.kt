package com.gblrod.encurtaai.repository

import com.gblrod.encurtaai.entity.Link
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface LinkRepository : JpaRepository<Link, Long> {
    fun existsByShortCode(shortCode: String): Boolean
    fun findByShortCodeAndDeletedAtIsNull(shortCode: String): Link?
    fun findByShortCodeAndDeletedAtIsNotNull(shortCode: String): Link?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Link>
    fun findAllByDeletedAtIsNotNull(pageable: Pageable): Page<Link>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
            DELETE FROM Link l
            WHERE l.deletedAt IS NOT NULL
            AND l.deletedAt <= :expirationThreshold
        """
    )

    fun deleteExpiredLinks(@Param("expirationThreshold") expirationThreshold: OffsetDateTime): Int
}