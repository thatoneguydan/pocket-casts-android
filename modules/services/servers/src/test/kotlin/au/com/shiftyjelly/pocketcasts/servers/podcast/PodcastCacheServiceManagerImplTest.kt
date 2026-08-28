package au.com.shiftyjelly.pocketcasts.servers.podcast

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastCacheServiceManagerImplTest {
    @Test
    fun `Gigachomper episode URL bypasses server refresh`() {
        assertTrue(
            isGigachomperPlaybackUrl(
                "https://dndkids.ddnsgeek.com/de-drakengardt/media/Session%2040.mp3",
            ),
        )
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
