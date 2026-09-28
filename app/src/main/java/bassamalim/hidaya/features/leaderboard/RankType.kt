package bassamalim.hidaya.features.leaderboard

/** [field] is the Firestore field in the Leaderboard collection that this ranking orders by. */
enum class RankType(val field: String) {
    BY_READING("reading_record"),
    BY_LISTENING("listening_record")
}
