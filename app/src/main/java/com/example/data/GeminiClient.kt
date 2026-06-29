package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun getMoshi(): Moshi = moshi
}

object GeminiClient {
    private const val DEFAULT_MODEL = "gemini-3.5-flash"

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            ""
        } else {
            key
        }
    }

    suspend fun analyzeResume(resumeText: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "API Key is missing! Please configure GEMINI_API_KEY in the Secrets Panel."
        }

        val prompt = """
            You are a professional hiring manager and CV analysis expert. Analyze the following candidate CV/resume text:
            
            $resumeText
            
            Provide:
            1. Key strengths identified (Bullet points)
            2. High-impact areas for improvement and gaps (Bullet points)
            3. Actionable optimization suggestions (e.g., formatting, keywords to add, impact verbs)
            4. Recommended job roles and industries that fit this background.
            
            Format your response in a clear, beautiful, and structured markdown style with emojis. Use professional, clean wording.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(DEFAULT_MODEL, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Received empty response from the AI model."
        } catch (e: Exception) {
            "Failed to analyze resume: ${e.localizedMessage}"
        }
    }

    suspend fun generateQuestions(
        jobTitle: String,
        industry: String,
        resumeContext: String?
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext listOf(
                "API Key is missing!",
                "Please configure GEMINI_API_KEY",
                "in your Secrets Panel",
                "to generate real-career-focused AI questions",
                "and practice mock interviews."
            )
        }

        val contextInfo = if (!resumeContext.isNullOrBlank()) {
            "Candidate Resume Context:\n$resumeContext"
        } else {
            "No CV provided. Generate generic professional questions."
        }

        val prompt = """
            You are an expert technical interviewer. Generate exactly 5 realistic, high-fidelity interview questions for the role:
            Position: $jobTitle
            Industry: $industry
            $contextInfo
            
            Include a mix of behavioral (STAR method), technical, and situational questions tailored perfectly to the job level and industry.
            Return the output in a clean, valid JSON format matching this schema:
            {
              "questions": ["Question 1", "Question 2", "Question 3", "Question 4", "Question 5"]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                responseMimeType = "application/json"
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(DEFAULT_MODEL, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext emptyList()
            
            val adapter = RetrofitClient.getMoshi().adapter(QuestionsListResult::class.java)
            val result = adapter.fromJson(jsonText)
            result?.questions ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback questions if API or parsing fails
            listOf(
                "Tell me about a challenging professional project you completed and how you overcame technical roadblocks.",
                "How do you stay updated with the latest trends and tools in the $industry industry?",
                "Describe a situation where you had to work with a difficult stakeholders or team member. How did you handle it?",
                "What is your approach to testing and ensuring the quality of your deliverables as a $jobTitle?",
                "Why are you interested in a $jobTitle role in the $industry industry, and what unique value do you bring?"
            )
        }
    }

    suspend fun evaluateInterviewSession(
        jobTitle: String,
        industry: String,
        questionsAndAnswers: List<Pair<String, String>>
    ): SessionEvaluationResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext null
        }

        val qaText = questionsAndAnswers.joinToString("\n\n") { (q, a) ->
            "Question: ${q}\nUser Answer: ${a.ifBlank { "[No answer provided]" }}"
        }

        val prompt = """
            You are a senior executive recruiter. Grade the candidate's answers from their mock interview session:
            Role: $jobTitle
            Industry: $industry
            
            Interview Transcript:
            $qaText
            
            Analyze each answer for technical accuracy, clarity, and structure (such as STAR method).
            Then, determine an overall score from 0 to 100 and write a helpful summary feedback.
            
            Return your complete evaluation in valid JSON matching this exact structure:
            {
              "overallScore": 82,
              "overallFeedback": "Your summary goes here...",
              "questionFeedbacks": [
                {
                  "questionNumber": 1,
                  "score": 85,
                  "feedback": "Feedback for Question 1",
                  "modelAnswer": "What progress looks like/Ideal Answer"
                }
              ]
            }
            Ensure the size of 'questionFeedbacks' array matches the number of questions in the transcript.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.5f,
                responseMimeType = "application/json"
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(DEFAULT_MODEL, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext null
            
            val adapter = RetrofitClient.getMoshi().adapter(SessionEvaluationResult::class.java)
            adapter.fromJson(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
