package com.familyevents.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.familyevents.data.AuthRepository
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import kotlinx.coroutines.launch

private const val MESSAGE_LIMIT = 500

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    googleSignInEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var showInviteDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    fun submitAuth(successMessage: String? = null, action: suspend () -> Unit) {
        statusMessage = null
        statusIsError = false
        isSubmitting = true
        scope.launch {
            try {
                action()
                statusMessage = successMessage
            } catch (error: Exception) {
                statusIsError = true
                statusMessage = error.message ?: "Request failed. Please try again."
            } finally {
                isSubmitting = false
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
        modifier = modifier
            .fillMaxSize()
            .padding(Tokens.Space.S5),
    ) {
        Text("Family Events", style = FamilyTypography.TitleLarge)
        Text("Sign in to plan your weekend.", style = FamilyTypography.Body)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { submitAuth { authRepository.signIn(email.trim(), password) } },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in")
        }
        OutlinedButton(
            onClick = { submitAuth { authRepository.signUp(email.trim(), password) } },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create account")
        }
        OutlinedButton(
            onClick = {
                submitAuth("Check your email for a reset link.") {
                    authRepository.resetPassword(email.trim())
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset password")
        }
        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (googleSignInEnabled) {
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Continue with Google")
            }
        }
        TextButton(
            onClick = { showInviteDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Request invite code")
        }
    }

    if (showInviteDialog) {
        InviteRequestDialog(
            initialEmail = email.trim(),
            authRepository = authRepository,
            onDismiss = { showInviteDialog = false },
        )
    }
}

@Composable
private fun InviteRequestDialog(
    initialEmail: String,
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var inviteEmail by remember { mutableStateOf(initialEmail) }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var successState by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val messageOverLimit = message.length > MESSAGE_LIMIT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request invite code") },
        text = {
            if (successState) {
                Text(
                    "Request received. We'll review and email you a code if it's a fit. Usually takes a day or two.",
                    style = FamilyTypography.Body,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1)) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message (optional)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${message.length}/$MESSAGE_LIMIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (messageOverLimit) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (successState) {
                Button(onClick = onDismiss) {
                    Text("Got it")
                }
            } else {
                Button(
                    onClick = {
                        val trimmedEmail = inviteEmail.trim()
                        val validationError = validateInviteRequest(trimmedEmail, message)
                        if (validationError != null) {
                            errorMessage = validationError
                            return@Button
                        }
                        errorMessage = null
                        isSubmitting = true
                        scope.launch {
                            try {
                                val ok = authRepository.requestInvite(
                                    trimmedEmail,
                                    message.trim().takeIf { it.isNotEmpty() },
                                )
                                if (ok) {
                                    successState = true
                                } else {
                                    errorMessage = "Couldn't submit your request. If you've requested recently, please wait a few minutes and try again."
                                }
                            } catch (e: Exception) {
                                errorMessage = "Couldn't submit your request. If you've requested recently, please wait a few minutes and try again."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting,
                ) {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            if (!successState) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

private fun validateInviteRequest(email: String, message: String): String? = when {
    email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email address."
    message.length > MESSAGE_LIMIT -> "Message must be 500 characters or fewer."
    else -> null
}
