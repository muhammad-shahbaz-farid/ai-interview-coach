package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import kotlinx.coroutines.launch

@Composable
fun SimpleMarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("###") -> {
                    Text(
                        text = trimmed.removePrefix("###").trim(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("##") -> {
                    Text(
                        text = trimmed.removePrefix("##").trim(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("#") -> {
                    Text(
                        text = trimmed.removePrefix("#").trim(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )
                }
                trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val content = if (trimmed.startsWith("-")) trimmed.removePrefix("-").trim() else trimmed.removePrefix("*").trim()
                    val boldParts = content.split("**")
                    Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, end = 4.dp)) {
                        Text("• ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        val annotatedStr = buildAnnotatedString {
                            boldParts.forEachIndexed { idx, part ->
                                if (idx % 2 == 1) {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                                        append(part)
                                    }
                                } else {
                                    append(part)
                                }
                            }
                        }
                        Text(text = annotatedStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                else -> {
                    if (trimmed.isNotEmpty()) {
                        val boldParts = trimmed.split("**")
                        val annotatedStr = buildAnnotatedString {
                            boldParts.forEachIndexed { idx, part ->
                                if (idx % 2 == 1) {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                                        append(part)
                                    }
                                } else {
                                    append(part)
                                }
                            }
                        }
                        Text(
                            text = annotatedStr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: InterviewViewModel,
    speakText: (String) -> Unit,
    startListening: (onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    stopListening: () -> Unit,
    isListening: Boolean
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "AI Interview Coach",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    if (currentScreen != ScreenStatus.Dashboard) {
                        IconButton(onClick = { viewModel.navigateTo(ScreenStatus.Dashboard) }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenStatus.ProfileSetup) }) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profile Setup")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    is ScreenStatus.Dashboard -> DashboardView(viewModel)
                    is ScreenStatus.ProfileSetup -> ProfileSetupView(viewModel)
                    is ScreenStatus.ActiveInterview -> ActiveInterviewView(
                        viewModel = viewModel,
                        sessionId = targetScreen.sessionId,
                        speakText = speakText,
                        startListening = startListening,
                        stopListening = stopListening,
                        isListening = isListening
                    )
                    is ScreenStatus.Evaluation -> EvaluationView(viewModel, targetScreen.sessionId)
                    is ScreenStatus.SessionList -> SessionListView(viewModel)
                }
            }
        }
    }
}

@Composable
fun DashboardView(viewModel: InterviewViewModel) {
    val profile by viewModel.profile.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val scrollState = rememberScrollState()

    var targetRole by remember { mutableStateOf("") }
    var targetIndustry by remember { mutableStateOf("") }

    // Prefill form inputs if profile exists
    LaunchedEffect(profile) {
        profile?.let {
            if (targetRole.isEmpty()) targetRole = it.targetRole
            if (targetIndustry.isEmpty()) targetIndustry = it.targetIndustry
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image Illustration
        Image(
            painter = painterResource(id = R.drawable.img_interview_banner),
            contentDescription = "AI Coach Banner",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        // Hello Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Welcome back, ${profile?.name?.ifBlank { "Job Seeker" } ?: "Future Graduate"}!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Let's practice and analyze your performance to secure your dream professional career. Start a customized mock interview session below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // CV Profile Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CV Coach Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    TextButton(onClick = { viewModel.navigateTo(ScreenStatus.ProfileSetup) }) {
                        Text(if (profile == null) "Create Profile" else "Edit / View CV Analysis")
                    }
                }

                if (profile != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Target Role: ")
                            }
                            append(profile!!.targetRole)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Industry: ")
                            }
                            append(profile!!.targetIndustry)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You haven't uploaded or analyzed your resume context yet. Uploading a resume allows Gemini to generate questions specific to your background!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Start New Mock Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Prepare Mock Interview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Key in your customized target position to instantly generate realistic AI questions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = targetRole,
                    onValueChange = { targetRole = it },
                    label = { Text("Target Job Title") },
                    placeholder = { Text("e.g. Android Developer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetIndustry,
                    onValueChange = { targetIndustry = it },
                    label = { Text("Target Industry") },
                    placeholder = { Text("e.g. Technology") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = "Generating tailored AI questions...",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (targetRole.isNotBlank() && targetIndustry.isNotBlank()) {
                                viewModel.startNewInterview(targetRole, targetIndustry)
                            }
                        },
                        enabled = targetRole.isNotBlank() && targetIndustry.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Launch AI Mock Interview")
                    }
                }
            }
        }

        // Recent Sessions Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Mock Practices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            if (sessions.size > 3) {
                TextButton(onClick = { viewModel.navigateTo(ScreenStatus.SessionList) }) {
                    Text("View All")
                }
            }
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No practice sessions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            sessions.take(3).forEach { session ->
                SessionItemRow(session = session, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SessionItemRow(session: InterviewSession, viewModel: InterviewViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (session.completed) {
                    viewModel.navigateTo(ScreenStatus.Evaluation(session.id))
                } else {
                    viewModel.navigateTo(ScreenStatus.ActiveInterview(session.id))
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.jobTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.industry,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedDate = java.text.DateFormat.getDateInstance()
                        .format(java.util.Date(session.timestamp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (session.completed) "Completed" else "In Progress",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (session.completed) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (session.completed) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${session.overallScore}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSetupView(viewModel: InterviewViewModel) {
    val profile by viewModel.profile.collectAsState()

    var name by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("") }
    var targetIndustry by remember { mutableStateOf("") }
    var resumeText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            targetRole = it.targetRole
            targetIndustry = it.targetIndustry
            resumeText = it.resumeText
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome title
        Text(
            text = "Create Professional CV Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Enter your career target details along with your current CV text context. The AI coach will analyze your resume strengths/drawbacks and construct personalized interviews based on this data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Alinah Joel") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = targetRole,
            onValueChange = { targetRole = it },
            label = { Text("Target Role / Profession") },
            placeholder = { Text("e.g. Software Engineer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = targetIndustry,
            onValueChange = { targetIndustry = it },
            label = { Text("Target Industry") },
            placeholder = { Text("e.g. Information Technology") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = resumeText,
            onValueChange = { resumeText = it },
            label = { Text("Resume / CV Text Content") },
            placeholder = { Text("Paste your work experience, education, skills, and target career description here...") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (name.isNotBlank() && targetRole.isNotBlank() && targetIndustry.isNotBlank() && resumeText.isNotBlank()) {
                    viewModel.saveProfile(name, targetRole, targetIndustry, resumeText)
                    showDialog = true
                }
            },
            enabled = name.isNotBlank() && targetRole.isNotBlank() && targetIndustry.isNotBlank() && resumeText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze Resume & Save Profile")
        }

        if (profile?.cvAnalysis != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gemini AI Resume Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(modifier = Modifier.padding(bottom = 12.dp))
                    
                    if (profile?.cvAnalysis == "Analyzing CV... Please wait a moment.") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyzing details via Gemini...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        SimpleMarkdownText(text = profile!!.cvAnalysis ?: "")
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Profile Updated") },
            text = { Text("Your resume has been submitted for analysis! The AI coach is currently processing your job strengths, suggestions, and tips. Feel free to explore details in a few moments!") },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ActiveInterviewView(
    viewModel: InterviewViewModel,
    sessionId: Int,
    speakText: (String) -> Unit,
    startListening: (onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    stopListening: () -> Unit,
    isListening: Boolean
) {
    val session by viewModel.getSessionFlow(sessionId).collectAsState(null)
    val questions by viewModel.getQuestionsFlow(sessionId).collectAsState(emptyList())
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val isEvaluating by viewModel.isEvaluating.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()

    var userAnswerText by remember { mutableStateOf("") }
    var sttErrorMessage by remember { mutableStateOf("") }

    val activeQuestion = questions.getOrNull(currentIndex)

    // Trigger Text To Speech whenever the active question changes
    LaunchedEffect(activeQuestion, voiceEnabled) {
        activeQuestion?.let {
            if (voiceEnabled) {
                speakText(it.text)
            }
        }
    }

    // Capture user answer input changes to the text field model
    LaunchedEffect(activeQuestion) {
        userAnswerText = activeQuestion?.userAnswer ?: ""
        sttErrorMessage = ""
    }

    if (session == null || questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(
                    text = "Loading mock interview questions...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Row (Current question index + Job Title)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session!!.jobTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = session!!.industry,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Q ${currentIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Horizontal Progress Bar
        val progress = (currentIndex + 1).toFloat() / questions.size
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        // Simulated Avatar soundwave graphics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Coach Icon Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "AI Recruiter",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "AI Interviewer Coach",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isListening) "Listening carefully..." else "Ready to listen",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isListening) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    // Text-To-Speech Speaking toggle
                    IconButton(onClick = { viewModel.setVoiceEnabled(!voiceEnabled) }) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Mute",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speaking Ripple Wave Animations
                if (isListening) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { i ->
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(if (i % 2 == 0) 24.dp else 14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Actual generated Question UI
                activeQuestion?.let {
                    Text(
                        text = it.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text-To-Speech Pronunciation trigger
                    FilledTonalButton(
                        onClick = { speakText(it.text) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Play Question")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Replay Question Voice")
                    }
                }
            }
        }

        // Answer area
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Your Answer Response",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Type your response or tap the microphone to utilize live voice transcription conversation:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = userAnswerText,
                    onValueChange = {
                        userAnswerText = it
                        activeQuestion?.let { q -> viewModel.updateQuestionAnswer(q, it) }
                    },
                    placeholder = { Text("Enter your response details here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (sttErrorMessage.isNotEmpty()) {
                    Text(
                        text = sttErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clean Voice transcription live controls
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            if (isListening) {
                                stopListening()
                            } else {
                                sttErrorMessage = ""
                                startListening(
                                    { textTranscribed ->
                                        userAnswerText = textTranscribed
                                        activeQuestion?.let { q -> viewModel.updateQuestionAnswer(q, textTranscribed) }
                                    },
                                    { errorMsg ->
                                        sttErrorMessage = errorMsg
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isListening) "Stop Speech Recording" else "Vocalize Converse (Live Voice)")
                    }
                }
            }
        }

        // Navigation actions block
        if (isEvaluating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Analyzing responses & evaluating scores...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary action: Previous
                TextButton(
                    onClick = {
                        if (currentIndex > 0) {
                            viewModel.setQuestionIndex(currentIndex - 1)
                        }
                    },
                    enabled = currentIndex > 0
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Prev")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previous")
                }

                // Primary action: Next / Finish
                Button(
                    onClick = {
                        if (currentIndex < questions.size - 1) {
                            viewModel.setQuestionIndex(currentIndex + 1)
                        } else {
                            viewModel.evaluateSession(sessionId)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (currentIndex < questions.size - 1) {
                        Text("Next Question")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Next")
                    } else {
                        Text("Finish & Evaluate Score")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun EvaluationView(viewModel: InterviewViewModel, sessionId: Int) {
    val session by viewModel.getSessionFlow(sessionId).collectAsState(null)
    val questions by viewModel.getQuestionsFlow(sessionId).collectAsState(emptyList())

    if (session == null || !session!!.completed) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Analyzing score card...", modifier = Modifier.padding(top = 12.dp))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Summary Header Card with Radial Score indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ring score indicators
                val ringColor = when {
                    session!!.overallScore >= 80 -> Color(0xFF2E7D32)
                    session!!.overallScore >= 60 -> Color(0xFFF57C00)
                    else -> Color(0xFFD32F2F)
                }

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = 0.15f))
                        .border(4.dp, ringColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${session!!.overallScore}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ringColor
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Practice Complete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = session!!.jobTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Score card calibrated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Overall Feedback Text Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Overall Performance Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                SimpleMarkdownText(text = session!!.overallFeedback)
            }
        }

        // Expandable List showing Question by Question detailed evaluation
        Text(
            text = "Question Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        questions.forEach { question ->
            var expanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${question.questionNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        (if (question.score ?: 0 >= 80) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                                            .copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${question.score ?: 0} pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (question.score ?: 0 >= 80) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand"
                            )
                        }
                    }

                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (expanded) {
                        Divider(modifier = Modifier.padding(vertical = 10.dp))
                        
                        Text(
                            text = "Your Answer Response:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = question.userAnswer?.ifBlank { "[No response provided]" } ?: "[No response provided]",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Text(
                            text = "AI Recruiter Evaluation:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = question.feedback ?: "No question feedback available.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Text(
                            text = "Ideal Sample / Model Answer Reference:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = question.modelAnswer ?: "No reference answer details calibrated.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Return button Action
        Button(
            onClick = { viewModel.navigateTo(ScreenStatus.Dashboard) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Coach Dashboard")
        }
    }
}

@Composable
fun SessionListView(viewModel: InterviewViewModel) {
    val sessions by viewModel.allSessions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Practice History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sessions) { session ->
                SessionItemRow(session = session, viewModel = viewModel)
            }
        }
    }
}
