package com.example.campusbite.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbite.R
import com.example.campusbite.viewmodels.AuthViewModel
import com.example.campusbite.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (Boolean) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    var registrationSuccessMessage by remember { mutableStateOf("") }
    var hasNavigated by remember { mutableStateOf(false) }

    // Animated transitions
    val transitionState = remember {
        MutableTransitionState(isLoginMode)
    }
    transitionState.targetState = isLoginMode

    // Handle navigation
    LaunchedEffect(authState) {
        if (!hasNavigated) {
            when (authState) {
                is AuthState.Authenticated -> {
                    hasNavigated = true
                    val user = (authState as AuthState.Authenticated).user
                    val isAdmin = user.email == "admin@campusbite.com"
                    onLoginSuccess(isAdmin)
                }
                else -> { /* Do nothing */ }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { hasNavigated = false }
    }

    LaunchedEffect(registrationSuccessMessage) {
        if (registrationSuccessMessage.isNotEmpty()) {
            kotlinx.coroutines.delay(3000)
            registrationSuccessMessage = ""
        }
    }

    // Gradient background with subtle animation
    val infiniteTransition = rememberInfiniteTransition()
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF3E0),
                        Color(0xFFFFE0B2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 60.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Burger Image with bounce effect
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "burger_scale"
            )

            Image(
                painter = painterResource(id = R.drawable.burger),
                contentDescription = "Burger",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .scale(scale)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Title with fade-in
            AnimatedContent(
                targetState = isLoginMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                        fadeOut(animationSpec = tween(500))
                },
                label = "title_animation"
            ) { isLogin ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isLogin) "Welcome Back!" else "Join CampusBite",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1B0E),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isLogin) "Sign in to continue" else "Create your account to get started",
                        fontSize = 14.sp,
                        color = Color(0xFF8D6E63),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Animated White Card Container with slide-in and scale
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(800)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(800, easing = FastOutSlowInEasing)
                        ) +
                        scaleIn(initialScale = 0.9f, animationSpec = tween(800)),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Name Field (registration only) with animated visibility
                        AnimatedVisibility(
                            visible = !isLoginMode,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    expandVertically(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200)) +
                                    shrinkVertically(animationSpec = tween(200))
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Full Name", color = Color(0xFFFF6B00)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF6B00),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        cursorColor = Color(0xFFFF6B00)
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Animated Email Field with focus effect
                        val emailFieldColor by animateColorAsState(
                            targetValue = if (email.isNotEmpty()) Color(0xFFFF6B00) else Color(0xFFE0E0E0),
                            animationSpec = tween(300),
                            label = "email_field_color"
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", color = Color(0xFFFF6B00)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF6B00),
                                unfocusedBorderColor = emailFieldColor,
                                cursorColor = Color(0xFFFF6B00)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color(0xFFFF6B00)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF6B00),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                cursorColor = Color(0xFFFF6B00)
                            )
                        )

                        // Forgot Password? (Login mode only) with animated visibility
                        AnimatedVisibility(
                            visible = isLoginMode,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(200)) +
                                    slideOutHorizontally(animationSpec = tween(200))
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { /* Handle forgot password */ },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF6B00)
                                )
                            }
                        }

                        // Error / Success Messages with animated visibility
                        AnimatedVisibility(
                            visible = errorMessage.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(300)) +
                                    expandVertically(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300)) +
                                    shrinkVertically(animationSpec = tween(300))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = registrationSuccessMessage.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(300)) +
                                    expandVertically(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300)) +
                                    shrinkVertically(animationSpec = tween(300))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = registrationSuccessMessage,
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Animated Button with pulse effect when loading
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isLoading) 0.98f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "button_scale"
                        )

                        Button(
                            onClick = {
                                if (isLoginMode) {
                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        authViewModel.login(email, password)
                                    }
                                } else {
                                    if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                                        authViewModel.register(name, email, password)
                                        registrationSuccessMessage = "✅ Registration successful! Please login."
                                        name = ""
                                        email = ""
                                        password = ""
                                        isLoginMode = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(buttonScale),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B00),
                                disabledContainerColor = Color(0xFFFFB74D)
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isLoginMode) "Login" else "Register",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider with "or"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0),
                                thickness = 1.dp
                            )
                            Text(
                                text = "  or  ",
                                color = Color(0xFFBDBDBD),
                                fontSize = 12.sp
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0),
                                thickness = 1.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Toggle between Login and Register with animated transition
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                                color = Color(0xFF8D6E63),
                                fontSize = 14.sp
                            )
                            TextButton(
                                onClick = {
                                    isLoginMode = !isLoginMode
                                    name = ""
                                    email = ""
                                    password = ""
                                    registrationSuccessMessage = ""
                                },
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Text(
                                    text = if (isLoginMode) "Register" else "Login",
                                    color = Color(0xFFFF6B00),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Animated Footer text
            val footerAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(1000, delayMillis = 500),
                label = "footer_alpha"
            )

            Text(
                text = "Made with ❤️ for Campus Students",
                fontSize = 12.sp,
                color = Color(0xFF8D6E63),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .alpha(footerAlpha)
            )
        }
    }
}