package com.gblrod.encurtaai.service

import com.gblrod.encurtaai.policy.LinkRecoveryPolicy
import com.gblrod.encurtaai.repository.LinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class LinkCleanupService(private val repository: LinkRepository) {
    @Transactional
    fun deleteExpiredLinks(): Int {
        val expirationThreshold = OffsetDateTime.now().minusDays(LinkRecoveryPolicy.RETENTION_PERIOD_DAYS)
        return repository.deleteExpiredLinks(expirationThreshold)
    }
}