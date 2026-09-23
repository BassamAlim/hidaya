package bassamalim.hidaya.features.recitations

import bassamalim.hidaya.features.recitations.RecitationMediaId.Parts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecitationMediaIdTest {

    @Test
    fun `round-trips ids above 999`() {
        val id = RecitationMediaId.encode(reciterId = 21182, narrationId = 10904, suraIdx = 3)
        assertEquals(Parts(21182, 10904, 3), RecitationMediaId.decode(id))
    }

    @Test
    fun `decodes legacy fixed-width ids`() {
        assertEquals(Parts(5, 12, 113), RecitationMediaId.decode("005012113"))
    }

    @Test
    fun `rejects malformed ids`() {
        assertNull(RecitationMediaId.decode(""))
        assertNull(RecitationMediaId.decode("00000000"))
        assertNull(RecitationMediaId.decode("2118210904003"))  // corrupted legacy id from the crash
        assertNull(RecitationMediaId.decode("1-2"))
        assertNull(RecitationMediaId.decode("a-2-3"))
    }

}
