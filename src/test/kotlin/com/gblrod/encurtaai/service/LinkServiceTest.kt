package com.gblrod.encurtaai.service

import com.gblrod.encurtaai.config.AppProperties
import com.gblrod.encurtaai.config.PaginationProperties
import com.gblrod.encurtaai.dto.CreateLinkRequestDto
import com.gblrod.encurtaai.entity.Link
import com.gblrod.encurtaai.exception.LinkNotFoundException
import com.gblrod.encurtaai.exception.LinkRecoveryExpiredException
import com.gblrod.encurtaai.exception.ShortCodeAlreadyExistsException
import com.gblrod.encurtaai.policy.LinkRecoveryPolicy
import com.gblrod.encurtaai.repository.LinkRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkServiceTest {
    private val linkRepository = Mockito.mock(LinkRepository::class.java)
    private val codeGenerator = Mockito.mock(CodeGenerator::class.java)
    private val appProperties = AppProperties(baseUrl = "http://localhost:8080")
    private val paginationProperties = PaginationProperties(
        defaultSize = 25,
        maxSize = 200,
        defaultSort = "createdAt",
        defaultDirection = "DESC"
    )

    private lateinit var linkService: LinkService

    @BeforeEach
    fun setUp() {
        linkService = LinkService(
            repository = linkRepository,
            codeGenerator = codeGenerator,
            appProperties = appProperties,
            paginationProperties = paginationProperties
        )
    }

    @Test
    fun shouldCreateLinkWithGeneratedShortCode() {
        val url = "https://youtube.com"
        val localHost = "http://localhost:8080"
        val generatedShortCode = "yt2026"
        val request = CreateLinkRequestDto(url = url, shortCode = null)

        Mockito.`when`(codeGenerator.generate()).thenReturn(generatedShortCode)
        Mockito.`when`(linkRepository.existsByShortCode(generatedShortCode)).thenReturn(false)
        Mockito.`when`(linkRepository.save(Mockito.any(Link::class.java)))
            .thenAnswer { invocation -> invocation.getArgument<Link>(0) }

        val result = linkService.create(request)

        assertEquals(url, result.originalUrl)
        assertEquals(generatedShortCode, result.shortCode)
        assertEquals("$localHost/$generatedShortCode", result.shortUrl)
    }

    @Test
    fun shouldCreateLinkWithCustomShortCode() {
        val url = "https://youtube.com"
        val localHost = "http://localhost:8080"
        val generatedShortCode = "yt2026"
        val request = CreateLinkRequestDto(url = url, shortCode = generatedShortCode)

        Mockito.`when`(linkRepository.existsByShortCode(generatedShortCode)).thenReturn(false)
        Mockito.`when`(linkRepository.save(Mockito.any(Link::class.java)))
            .thenAnswer { invocation -> invocation.getArgument<Link>(0) }

        val result = linkService.create(request)

        assertEquals(url, result.originalUrl)
        assertEquals(generatedShortCode, result.shortCode)
        assertEquals("$localHost/$generatedShortCode", result.shortUrl)
    }

    @Test
    fun shouldRejectDuplicateCustomShortCode() {
        val url = "https://youtube.com"
        val generatedShortCode = "yt2026"
        val requestOne = CreateLinkRequestDto(url = url, shortCode = generatedShortCode)

        Mockito.`when`(linkRepository.existsByShortCode(generatedShortCode)).thenReturn(true)

        assertThrows<ShortCodeAlreadyExistsException> { linkService.create(requestOne) }

        Mockito.verify(linkRepository).existsByShortCode(generatedShortCode)
        Mockito.verify(linkRepository, Mockito.never()).save(Mockito.any(Link::class.java))
        Mockito.verifyNoInteractions(codeGenerator)
    }

    @Test
    fun shouldRetryCodeGenerationWhenGeneratedCodeAlreadyExists() {
        val url = "https://youtube.com"
        val generatedShortCodeOne = "yt2026"
        val generatedShortCodeTwo = "yt2027"
        val requestOne = CreateLinkRequestDto(url = url, shortCode = null)

        Mockito.`when`(codeGenerator.generate()).thenReturn(generatedShortCodeOne, generatedShortCodeTwo)
        Mockito.`when`(linkRepository.existsByShortCode(generatedShortCodeOne)).thenReturn(true)
        Mockito.`when`(linkRepository.existsByShortCode(generatedShortCodeTwo)).thenReturn(false)
        Mockito.`when`(linkRepository.save(Mockito.any(Link::class.java)))
            .thenAnswer { invocation -> invocation.getArgument<Link>(0) }

        val result = linkService.create(requestOne)

        assertEquals(generatedShortCodeTwo, result.shortCode)

        Mockito.verify(codeGenerator, Mockito.times(2)).generate()
        Mockito.verify(linkRepository).save(Mockito.any(Link::class.java))
    }

    @Test
    fun shouldIncrementAccessCountWhenRegisteringAccess() {
        val url = "https://youtube.com"
        val generatedShortCodeOne = "yt2026"
        val now = OffsetDateTime.now()
        val previousLastAccessedAt = now.minusDays(1)

        val link = Link(
            originalUrl = url,
            shortCode = generatedShortCodeOne,
            createdAt = now.minusDays(2)
        ).apply {
            accessCount = 3
            lastAccessedAt = previousLastAccessedAt
        }

        Mockito.`when`(linkRepository.findByShortCodeAndDeletedAtIsNull(generatedShortCodeOne)).thenReturn(link)

        Mockito.`when`(
            linkRepository.save(Mockito.any(Link::class.java))
        ).thenAnswer { invocation -> invocation.getArgument<Link>(0) }

        val result = linkService.registerAccess(generatedShortCodeOne)

        assertEquals(4, result.accessCount)
        assertNotNull(result.lastAccessedAt)
        assertTrue(result.lastAccessedAt!!.isAfter(previousLastAccessedAt))

        Mockito.verify(linkRepository).save(link)
    }

    @Test
    fun shouldSoftDeleteLink() {
        val shortCode = "yt2026"
        val now = OffsetDateTime.now()

        val link = Link(
            originalUrl = "https://youtube.com",
            shortCode = shortCode,
            createdAt = now
        )

        Mockito.`when`(linkRepository.findByShortCodeAndDeletedAtIsNull(shortCode)).thenReturn(link)

        linkService.delete(shortCode)
        assertNotNull(link.deletedAt)

        Mockito.verify(linkRepository).findByShortCodeAndDeletedAtIsNull(shortCode)
        Mockito.verify(linkRepository).save(link)
    }

    @Test
    fun shouldThrowNotFoundWhenDeletingAlreadyDeletedLink() {
        val shortCode = "yt2026"

        Mockito.`when`(linkRepository.findByShortCodeAndDeletedAtIsNull(shortCode)).thenReturn(null)
        assertThrows<LinkNotFoundException> { linkService.delete(shortCode) }

        Mockito.verify(linkRepository).findByShortCodeAndDeletedAtIsNull(shortCode)
        Mockito.verify(linkRepository, Mockito.never()).save(Mockito.any(Link::class.java))
    }

    @Test
    fun shouldReturnDeletedLinksWithPagination() {
        val shortCode = "yt2026"
        val secondShortCode = "github"
        val now = OffsetDateTime.now()
        val deletedAt = now.minusDays(2)

        val link = Link(
            originalUrl = "https://youtube.com",
            shortCode = shortCode,
            createdAt = now.minusDays(20),
            deletedAt = deletedAt
        )

        val secondLink = Link(
            originalUrl = "https://github.com/gblrodrigues",
            shortCode = secondShortCode,
            createdAt = now.minusDays(21),
            lastAccessedAt = now.minusDays(3),
            deletedAt = deletedAt
        )

        val pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
        Mockito.`when`(linkRepository.findAllByDeletedAtIsNotNull(pageable))
            .thenReturn(PageImpl(listOf(link, secondLink), pageable, 5))

        val result = linkService.findTrash(page = 1, size = 2, sort = "createdAt,DESC")
        Mockito.verify(linkRepository).findAllByDeletedAtIsNotNull(pageable)

        assertEquals(2, result.data.size)
        assertEquals(shortCode, result.data.first().shortCode)
        assertEquals(deletedAt, result.data.first().deletedAt)
        assertEquals(1, result.pagination.page)
        assertEquals(2, result.pagination.size)
        assertEquals(5, result.pagination.totalItems)
        assertEquals(3, result.pagination.totalPages)
    }

    @Test
    fun shouldReturnEmptyTrashWhenNoLinksAreDeleted() {
        val pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt"))
        Mockito.`when`(linkRepository.findAllByDeletedAtIsNotNull(pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = linkService.findTrash(page = 0, size = 25, sort = "createdAt,DESC")
        Mockito.verify(linkRepository).findAllByDeletedAtIsNotNull(pageable)

        assertTrue(result.data.isEmpty())
        assertEquals(0, result.pagination.totalItems)
        assertEquals(0, result.pagination.totalPages)
    }

    @Test
    fun shouldRestoreLinkWithinRecoveryPeriod() {
        val shortCode = "yt2026"
        val now = OffsetDateTime.now()

        val link = Link(
            originalUrl = "https://youtube.com",
            shortCode = shortCode,
            createdAt = now.minusDays(5),
            deletedAt = now.minusDays(1)
        )

        Mockito.`when`(linkRepository.findByShortCodeAndDeletedAtIsNotNull(shortCode)).thenReturn(link)
        Mockito.`when`(linkRepository.save(link)).thenReturn(link)

        val result = linkService.restore(shortCode)
        Mockito.verify(linkRepository).findByShortCodeAndDeletedAtIsNotNull(shortCode)
        Mockito.verify(linkRepository).save(link)

        assertNull(link.deletedAt)
        assertEquals(shortCode, result.shortCode)
    }

    @Test
    fun shouldPreserveLinkDataWhenRestoring() {
        val shortCode = "yt2026"
        val now = OffsetDateTime.now()

        val link = Link(
            originalUrl = "https://youtube.com",
            shortCode = shortCode,
            createdAt = now.minusDays(23),
            accessCount = 42,
            lastAccessedAt = now.minusDays(21),
            deletedAt = now.minusDays(20)
        )

        val originalId = link.id
        val originalUrl = link.originalUrl
        val originalShortCode = link.shortCode
        val originalAccessCount = link.accessCount
        val originalLastAccessedAt = link.lastAccessedAt

        Mockito.`when`(linkRepository.findByShortCodeAndDeletedAtIsNotNull(shortCode)).thenReturn(link)
        Mockito.`when`(linkRepository.save(link)).thenReturn(link)

        linkService.restore(shortCode)

        assertEquals(originalId, link.id)
        assertEquals(originalUrl, link.originalUrl)
        assertEquals(originalShortCode, link.shortCode)
        assertEquals(originalAccessCount, link.accessCount)
        assertEquals(originalLastAccessedAt, link.lastAccessedAt)
        assertNull(link.deletedAt)

        Mockito.verify(linkRepository).save(link)
    }

    @Test
    fun shouldRejectRestoringExpiredLink() {
        val shortCode = "yt2026"
        val now = OffsetDateTime.now()

        val link = Link(
            originalUrl = "https://youtube.com",
            shortCode = shortCode,
            createdAt = now.minusDays(60),
            deletedAt = now.minusDays(LinkRecoveryPolicy.RETENTION_PERIOD_DAYS + 1)
        )

        Mockito.`when`(linkRepository.findByShortCodeAndDeletedAtIsNotNull(shortCode)).thenReturn(link)
        assertThrows(LinkRecoveryExpiredException::class.java) { linkService.restore(shortCode) }
        Mockito.verify(linkRepository).findByShortCodeAndDeletedAtIsNotNull(shortCode)
        Mockito.verify(linkRepository, Mockito.never()).save(link)
    }

    @Test
    fun shouldThrowNotFoundWhenRestoringNonexistentLink() {
        val shortCode = "unknown"

        Mockito.`when`(linkRepository.findByShortCodeAndDeletedAtIsNotNull(shortCode)).thenReturn(null)
        assertThrows(LinkNotFoundException::class.java) { linkService.restore(shortCode) }
        Mockito.verify(linkRepository).findByShortCodeAndDeletedAtIsNotNull(shortCode)
        Mockito.verify(linkRepository, Mockito.never()).save(Mockito.any(Link::class.java))
    }

    @Test
    fun shouldExcludeDeletedLinksFromFindAll() {
        val now = OffsetDateTime.now()
        val activeLink = Link(
            originalUrl = "https://youtube.com",
            shortCode = "active",
            createdAt = now
        )

        val pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt"))
        Mockito.`when`(linkRepository.findAllByDeletedAtIsNull(pageable))
            .thenReturn(PageImpl(listOf(activeLink), pageable, 1))

        val result = linkService.findAll(page = 0, size = 25, sort = "createdAt,DESC")
        Mockito.verify(linkRepository).findAllByDeletedAtIsNull(pageable)

        assertEquals(1, result.data.size)
        assertEquals("active", result.data.first().shortCode)
    }
}