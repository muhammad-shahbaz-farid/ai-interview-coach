package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.InterviewQuestion
import com.example.data.InterviewRepository
import com.example.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenStatus {
    object Dashboard : ScreenStatus()
    object ProfileSetup : ScreenStatus()
    data class ActiveInterview(val sessionId: Int) : ScreenStatus()
    data class Evaluation(val sessionId: Int) : ScreenStatus()
    object SessionList : ScreenStatus()
}

class InterviewViewModel(private val repository: InterviewRepository) : ViewModel() {

    private val _currentScreen = MutableStateFlow<ScreenStatus>(ScreenStatus.Dashboard)
    val currentScreen: StateFlow<ScreenStatus> = _currentScreen.asStateFlow()

    val profile: StateFlow<UserProfile?> = repository.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSessions = repository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    fun navigateTo(screen: ScreenStatus) {
        _currentScreen.value = screen
        if (screen is ScreenStatus.ActiveInterview) {
            _currentQuestionIndex.value = 0
        }
    }

    fun saveProfile(name: String, role: String, industry: String, resumeText: String) {
        viewModelScope.launch {
            repository.saveProfile(name, role, industry, resumeText)
        }
    }

    fun startNewInterview(jobTitle: String, industry: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val sessionId = repository.startInterviewSession(jobTitle, industry)
                _currentQuestionIndex.value = 0
                _currentScreen.value = ScreenStatus.ActiveInterview(sessionId.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun updateQuestionAnswer(question: InterviewQuestion, answer: String) {
        viewModelScope.launch {
            repository.updateQuestionAnswer(question, answer)
        }
    }

    fun setQuestionIndex(index: Int) {
        _currentQuestionIndex.value = index
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _voiceEnabled.value = enabled
    }

    fun evaluateSession(sessionId: Int) {
        viewModelScope.launch {
            _isEvaluating.value = true
            try {
                val success = repository.evaluateSession(sessionId)
                if (success) {
                    _currentScreen.value = ScreenStatus.Evaluation(sessionId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isEvaluating.value = false
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentScreen.value is ScreenStatus.Evaluation && 
                (_currentScreen.value as ScreenStatus.Evaluation).sessionId == sessionId) {
                _currentScreen.value = ScreenStatus.Dashboard
            }
        }
    }

    fun getSessionFlow(sessionId: Int) = repository.getSessionFlow(sessionId)
    fun getQuestionsFlow(sessionId: Int) = repository.getQuestionsFlow(sessionId)
}

class InterviewViewModelFactory(private val repository: InterviewRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InterviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InterviewViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
