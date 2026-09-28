package bassamalim.hidaya.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookContent(
    @SerialName("bookInfo") val info: Info,
    @SerialName("chapters") val chapters: Array<Chapter>
) {

    @Serializable
    data class Info(
        @SerialName("bookId") val id: Int,
        @SerialName("bookTitle") val title: String,
        @SerialName("author") val author: String
    )

    @Serializable
    data class Chapter(
        @SerialName("chapterId") val id: Int,
        @SerialName("chapterTitle") val title: String,
        @SerialName("doors") val doors: Array<Door>
    ) {
        @Serializable
        class Door(
            @SerialName("doorId") val id: Int,
            @SerialName("doorTitle") val title: String,
            @SerialName("text") val text: String
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Chapter

            if (id != other.id) return false
            if (title != other.title) return false
            if (!doors.contentEquals(other.doors)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + title.hashCode()
            result = 31 * result + doors.contentHashCode()
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookContent

        if (info != other.info) return false
        if (!chapters.contentEquals(other.chapters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = info.hashCode()
        result = 31 * result + chapters.contentHashCode()
        return result
    }

}