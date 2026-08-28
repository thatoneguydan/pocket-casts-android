package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.mock

class PodcastCacheServiceManagerImplTest {
    @Test
    fun `Gigachomper episode URL bypasses server refresh`() = runTest {
        val service = mock<PodcastCacheService>()
        val manager = PodcastCacheServiceManagerImpl(service)
        val url = "https://dndkids.ddnsgeek.com/de-drakengardt/media/Session%2040.mp3"
        val episode = PodcastEpisode(
            uuid = "episode-id",
            publishedDate = Date(),
            podcastUuid = "podcast-id",
            downloadUrl = url,
        )

        assertEquals(url, manager.getEpisodeUrl(episode))
        verifyNoInteractions(service)
    }

    @Test
    fun `Gigachomper host match is case insensitive`() {
        assertTrue(
            isGigachomperPlaybackUrl(
                "https://DNDKIDS.DDNSGEEK.COM/de-drak-bonus/media/Session%2038%20Pre.mp3",
            ),
        )
    }

    @Test
    fun `ordinary podcast URL keeps server refresh`() {
        assertFalse(
            isGigachomperPlaybackUrl(
                "https://example.com/podcast/episode.mp3",
            ),
        )
    }

    @Test
    fun `lookalike hostname keeps server refresh`() {
        assertFalse(
            isGigachomperPlaybackUrl(
                "https://dndkids.ddnsgeek.com.example.com/podcast/episode.mp3",
            ),
        )
    }

    @Test
    fun `invalid URL keeps server refresh`() {
        assertFalse(isGigachomperPlaybackUrl("not a URL"))
    }
}
