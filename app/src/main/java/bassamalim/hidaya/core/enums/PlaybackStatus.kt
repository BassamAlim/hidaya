package bassamalim.hidaya.core.enums

/** What a play/pause button shows, derived from a Media3 player. */
enum class PlaybackStatus {
    /** Not connected to the player service yet. */
    CONNECTING,
    BUFFERING,
    PLAYING,
    PAUSED,
    /** Nothing loaded, or the queue played to its end. */
    STOPPED,
    ERROR
}
