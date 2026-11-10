package org.spotube.spotube.metadata

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.mockk.every
import kotlin.test.mockk.mockk
import kotlin.test.mockk.verify
import kotlin.test.mockk.exactly

@ExperimentalCoroutinesApi
class MetadataRepositoryTest {

    @Test
    fun `falls back to second source when first fails`() = runTest {
        val primary = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Primary"
        }
        val fallback = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Fallback"
        }

        every { primary.search("test") } throws Exception("Primary down")
        every { fallback.search("test") } returns SpotubeSearchResponse()

        val repo = MetadataRepository(listOf(primary, fallback))
        val result = repo.search("test")

        assertEquals(emptyList(), result.tracks)
        verify(exactly = 1) { primary.search("test") }
        verify(exactly = 1) { fallback.search("test") }
    }

    @Test
    fun `throws aggregate exception when all sources fail`() = runTest {
        val primary = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Primary"
        }
        val fallback = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Fallback"
        }

        every { primary.search("test") } throws Exception("Primary down")
        every { fallback.search("test") } throws Exception("Fallback down")

        val repo = MetadataRepository(listOf(primary, fallback))

        assertFailsWith<MetadataRepositoryException> { repo.search("test") }
    }

    @Test
    fun `skips unavailable sources`() = runTest {
        val unavailable = mockk<MetadataDataSource> {
            every { isAvailable } returns false
            every { name } returns "Unavailable"
        }
        val available = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Available"
        }

        every { available.search("test") } returns SpotubeSearchResponse()

        val repo = MetadataRepository(listOf(unavailable, available))
        repo.search("test")

        verify(exactly = 0) { unavailable.search(any()) }
        verify(exactly = 1) { available.search("test") }
    }

    @Test
    fun `write operations try each source until success`() = runTest {
        val primary = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Primary"
        }
        val fallback = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Fallback"
        }

        every { primary.saveArtists(listOf("artist1")) } throws Exception("Primary write failed")
        every { fallback.saveArtists(listOf("artist1")) } just Runs

        val repo = MetadataRepository(listOf(primary, fallback))
        repo.saveArtists(listOf("artist1"))

        verify(exactly = 1) { primary.saveArtists(listOf("artist1")) }
        verify(exactly = 1) { fallback.saveArtists(listOf("artist1")) }
    }

    @Test
    fun `isSavedTrack returns false if all sources fail`() = runTest {
        val primary = mockk<MetadataDataSource> {
            every { isAvailable } returns true
        }
        val fallback = mockk<MetadataDataSource> {
            every { isAvailable } returns true
        }

        every { primary.isSavedTrack("track1") } throws Exception("Primary down")
        every { fallback.isSavedTrack("track1") } throws Exception("Fallback down")

        val repo = MetadataRepository(listOf(primary, fallback))
        val result = repo.isSavedTrack("track1")

        assertFalse(result)
    }

    @Test
    fun `uses strategy from first source`() = runTest {
        val primary = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Primary"
            every { strategy } returns MetadataSourceStrategy.FASTEST_FIRST
        }
        val fallback = mockk<MetadataDataSource> {
            every { isAvailable } returns true
            every { name } returns "Fallback"
            every { strategy } returns MetadataSourceStrategy.PRIMARY_ONLY
        }

        val repo = MetadataRepository(listOf(primary, fallback))

        assertEquals(MetadataSourceStrategy.FASTEST_FIRST, repo.strategy)
    }
}