package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}

@Dao
interface InterviewDao {
    @Query("SELECT * FROM interview_session ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<InterviewSession>>

    @Query("SELECT * FROM interview_session WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Int): InterviewSession?

    @Query("SELECT * FROM interview_session WHERE id = :sessionId LIMIT 1")
    fun getSessionFlowById(sessionId: Int): Flow<InterviewSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InterviewSession): Long

    @Update
    suspend fun updateSession(session: InterviewSession)

    @Query("DELETE FROM interview_session WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    @Query("SELECT * FROM interview_question WHERE sessionId = :sessionId ORDER BY questionNumber ASC")
    fun getQuestionsFlowForSession(sessionId: Int): Flow<List<InterviewQuestion>>

    @Query("SELECT * FROM interview_question WHERE sessionId = :sessionId ORDER BY questionNumber ASC")
    suspend fun getQuestionsForSession(sessionId: Int): List<InterviewQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<InterviewQuestion>)

    @Update
    suspend fun updateQuestion(question: InterviewQuestion)
}

@Database(entities = [UserProfile::class, InterviewSession::class, InterviewQuestion::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun interviewDao(): InterviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "interview_coach_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
