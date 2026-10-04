package bassamalim.hidaya.core.data.dataSources.room.daos

import androidx.room.Dao
import androidx.room.Query
import bassamalim.hidaya.core.data.dataSources.room.entities.QuizQuestion

@Dao
interface QuizQuestionsDao {

    @Query("SELECT * FROM quiz_questions")
    fun getAll(): List<QuizQuestion>

    @Query("SELECT id FROM quiz_questions")
    fun getAllIds(): List<Int>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    fun getCount(): Int

    @Query("SELECT id FROM quiz_questions WHERE type_ar = :category OR type_en = :category")
    fun getCategoryIds(category: String): List<Int>

    @Query("SELECT * FROM quiz_questions WHERE id = :id")
    fun getQuestion(id: Int): QuizQuestion

    @Query("SELECT DISTINCT type_ar FROM quiz_questions WHERE type_ar IS NOT NULL")
    fun getTypes(): List<String>

}