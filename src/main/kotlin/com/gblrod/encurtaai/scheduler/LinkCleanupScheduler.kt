package com.gblrod.encurtaai.scheduler

import com.gblrod.encurtaai.service.LinkCleanupService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class LinkCleanupScheduler(private val cleanupService: LinkCleanupService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 3_600_000)
    fun cleanupExpiredLinks() {
        val deletedCount = cleanupService.deleteExpiredLinks()

        if (deletedCount > 0) {
            logger.info("Permanently deleted {} expired links.", deletedCount)
        }
    }
}