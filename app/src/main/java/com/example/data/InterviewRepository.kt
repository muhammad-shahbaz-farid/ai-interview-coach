package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class InterviewRepository(private val db: AppDatabase) {
    private val profileDao = db.userProfileDao()
    private val interviewDao = db.interviewDao()

    val profileFlow: Flow<UserProfile?> = profileDao.getProfileFlow()
    val allSessionsFlow: Flow<List<InterviewSession>> = interviewDao.getAllSessionsFlow()

    suspend fun getProfile(): UserProfile? = profileDao.getProfile()

    suspend fun saveProfile(
        name: String,
        role: String,
        industry: String,
        resumeText: String,
        onAnalysisComplete: () -> Unit = {}
    ) {
        val initialProfile = UserProfile(
            id = 1,
            name = name,
            targetRole = role,
            targetIndustry = industry,
            resumeText = resumeText,
            cvAnalysis = "Analyzing CV... Please wait a moment."
        )
        profileDao.insertOrUpdateProfile(initialProfile)

        // Run Gemini CV Analysis in the background
        val analysis = GeminiClient.analyzeResume(resumeText)
        val updatedProfile = initialProfile.copy(cvAnalysis = analysis)
        profileDao.insertOrUpdateProfile(updatedProfile)
        onAnalysisComplete()
    }

    fun getSessionFlow(sessionId: Int): Flow<InterviewSession?> =
        interviewDao.getSessionFlowById(sessionId)

    fun getQuestionsFlow(sessionId: Int): Flow<List<InterviewQuestion>> =
        interviewDao.getQuestionsFlowForSession(sessionId)

    suspend fun startInterviewSession(jobTitle: String, industry: String): Long {
        // Create active session in database first
        val session = InterviewSession(
            jobTitle = jobTitle,
            industry = industry,
            completed = false
        )
        val sessionId = interviewDao.insertSession(session).toInt()

        // Fetch resume context if profile exists
        val currentProfile = profileDao.getProfile()
        val resumeContext = currentProfile?.resumeText

        // Generate questions from Gemini (falls back to local checklist if offline or error)
        val questions = GeminiClient.generateQuestions(jobTitle, industry, resumeContext)

        val questionEntities = questions.mapIndexed { index, questionStr ->
            InterviewQuestion(
                sessionId = sessionId,
                questionNumber = index + 1,
                text = questionStr,
                userAnswer = ""
            )
        }
        
        interviewDao.insertQuestions(questionEntities)
        return sessionId.toLong()
    }

    suspend fun saveAnswer(questionId: Int, answer: String) {
        // Find existing question to preserve details, then update userAnswer
        // Flow/DAO provides specific helpers if needed or we update it directly:
        // For simplicity, we fetch all questions of session or find the question
        // Wait, updateQuestion accepts complete entity, so we read it or update it:
    }

    suspend fun updateQuestionAnswer(question: InterviewQuestion, answer: String) {
        val updated = question.copy(userAnswer = answer)
        interviewDao.updateQuestion(updated)
    }

    suspend fun deleteSession(sessionId: Int) {
        interviewDao.deleteSessionById(sessionId)
    }

    suspend fun evaluateSession(sessionId: Int): Boolean {
        val session = interviewDao.getSessionById(sessionId) ?: return false
        val questions = interviewDao.getQuestionsForSession(sessionId)
        if (questions.isEmpty()) return false

        val qas = questions.map { it.text to (it.userAnswer ?: "") }
        
        // Trigger evaluate from Gemini Client
        val result = GeminiClient.evaluateInterviewSession(session.jobTitle, session.industry, qas)
        if (result != null) {
            // Update individual questions
            val updatedQuestions = questions.map { original ->
                val eval = result.questionFeedbacks.find { it.questionNumber == original.questionNumber }
                original.copy(
                    feedback = eval?.feedback ?: "No question specific feedback provided.",
                    modelAnswer = eval?.modelAnswer ?: "No suggested answer provided.",
                    score = eval?.score ?: 70
                )
            }
            interviewDao.insertQuestions(updatedQuestions) // Re-inserts (REPLACE) on same primary keys

            // Update session
            val updatedSession = session.copy(
                overallScore = result.overallScore,
                overallFeedback = result.overallFeedback,
                completed = true
            )
            interviewDao.updateSession(updatedSession)
            return true
        } else {
            // Fallback evaluation if API fails or no keys available
            val averageScoreSize = questions.size
            var computedScore = 0
            val updatedQuestions = questions.map { original ->
                val lengthOfAnswer = original.userAnswer?.trim()?.length ?: 0
                val itemScore = when {
                    lengthOfAnswer > 150 -> 85
                    lengthOfAnswer > 50 -> 75
                    lengthOfAnswer > 0 -> 60
                    else -> 0
                }
                computedScore += itemScore
                original.copy(
                    feedback = "Answer length was $lengthOfAnswer characters. Good attempt! Detail could be improved using the STAR method (Situation, Task, Action, Result).",
                    modelAnswer = "To answer '${original.text}' perfectly, address it with a specific example highlighting your core actions and direct business results.",
                    score = itemScore
                )
            }
            interviewDao.insertQuestions(updatedQuestions)

            val finalScore = if (averageScoreSize > 0) computedScore / averageScoreSize else 60
            val updatedSession = session.copy(
                overallScore = finalScore,
                overallFeedback = "Evaluation calculated successfully offline. Tip: To get deep AI-driven grades, strengths, weaknesses, and model answers, please set up a real GEMINI_API_KEY value.",
                completed = true
            )
            interviewDao.updateSession(updatedSession)
            return true
        }
    }
}
