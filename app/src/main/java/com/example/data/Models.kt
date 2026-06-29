package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

// --- Room Database Entities ---

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val targetRole: String,
    val targetIndustry: String,
    val resumeText: String,
    val cvAnalysis: String? = null
)

@Entity(tableName = "interview_session")
data class InterviewSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobTitle: String,
    val industry: String,
    val timestamp: Long = System.currentTimeMillis(),
    val overallScore: Int = 0,
    val overallFeedback: String = "",
    val completed: Boolean = false
)

@Entity(tableName = "interview_question")
data class InterviewQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val questionNumber: Int,
    val text: String,
    val userAnswer: String? = null,
    val feedback: String? = null,
    val modelAnswer: String? = null,
    val score: Int? = null
)

// --- Moshi API Serialization Models ---

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// --- Moshi Structured JSON Output Parsers ---

@JsonClass(generateAdapter = true)
data class QuestionsListResult(
    val questions: List<String>
)

@JsonClass(generateAdapter = true)
data class QuestionEvaluation(
    val questionNumber: Int,
    val score: Int,
    val feedback: String,
    val modelAnswer: String
)

@JsonClass(generateAdapter = true)
data class SessionEvaluationResult(
    val overallScore: Int,
    val overallFeedback: String,
    val questionFeedbacks: List<QuestionEvaluation>
)
