package bassamalim.hidaya.features.recitations

/**
 * Recitation media id: "reciterId-narrationId-suraIdx".
 * Used to be fixed-width "RRRNNNSSS", which broke once reciter/narration ids passed 999.
 */
object RecitationMediaId {

    data class Parts(val reciterId: Int, val narrationId: Int, val suraIdx: Int)

    fun encode(reciterId: Int, narrationId: Int, suraIdx: Int) = "$reciterId-$narrationId-$suraIdx"

    /** Null if [mediaId] is malformed (e.g. a corrupted id saved by an older version). */
    fun decode(mediaId: String): Parts? {
        val parts =
            if ('-' in mediaId) mediaId.split('-')
            else if (mediaId.length == 9)  // legacy fixed-width
                listOf(mediaId.substring(0, 3), mediaId.substring(3, 6), mediaId.substring(6))
            else return null

        val ints = parts.map { it.toIntOrNull() ?: return null }
        if (ints.size != 3) return null
        return Parts(reciterId = ints[0], narrationId = ints[1], suraIdx = ints[2])
    }

}
