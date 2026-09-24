package bassamalim.hidaya.core.helpers

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import bassamalim.hidaya.core.enums.PlaybackStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/** How a player's state maps to what the play/pause buttons show. */
class PlaybackStatusTest {

    private fun player(
        state: Int,
        isPlaying: Boolean = false,
        playWhenReady: Boolean = false,
        error: PlaybackException? = null
    ) = mockk<Player> {
        every { playbackState } returns state
        every { this@mockk.isPlaying } returns isPlaying
        every { this@mockk.playWhenReady } returns playWhenReady
        every { playerError } returns error
    }

    @Test
    fun `playing`() {
        assertEquals(
            PlaybackStatus.PLAYING,
            player(Player.STATE_READY, isPlaying = true, playWhenReady = true).playbackStatus()
        )
    }

    @Test
    fun `ready but not playing is paused`() {
        assertEquals(PlaybackStatus.PAUSED, player(Player.STATE_READY).playbackStatus())
    }

    @Test
    fun `buffering shows a spinner only when it will play`() {
        assertEquals(
            PlaybackStatus.BUFFERING,
            player(Player.STATE_BUFFERING, playWhenReady = true).playbackStatus()
        )
        assertEquals(PlaybackStatus.PAUSED, player(Player.STATE_BUFFERING).playbackStatus())
    }

    @Test
    fun `idle and ended are stopped`() {
        assertEquals(PlaybackStatus.STOPPED, player(Player.STATE_IDLE).playbackStatus())
        assertEquals(PlaybackStatus.STOPPED, player(Player.STATE_ENDED).playbackStatus())
    }

    @Test
    fun `an error wins over everything else`() {
        assertEquals(
            PlaybackStatus.ERROR,
            player(Player.STATE_IDLE, error = mockk()).playbackStatus()
        )
    }

}
