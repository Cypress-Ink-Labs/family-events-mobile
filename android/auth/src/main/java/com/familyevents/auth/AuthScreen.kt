package com.familyevents.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.familyevents.data.AuthRepository
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

private const val MESSAGE_LIMIT = 500
private const val TAG = "FEAuth"

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    googleSignInEnabled: Boolean,
    googleWebClientId: String? = null,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findActivity()
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
        if (googleSignInEnabled && googleWebClientId != null && activity != null) {
            OutlinedButton(
                onClick = {
                    Log.d(TAG, "Google button tapped (webClientId prefix=${googleWebClientId.take(20)}…)")
                    submitAuth {
                        val idToken = requestGoogleIdToken(activity, googleWebClientId)
                        if (idToken == null) {
                            Log.w(TAG, "Google sign-in returned null id_token (cancelled or no credential)")
                            return@submitAuth
                        }
                        Log.d(TAG, "Google id_token received (len=${idToken.length}), calling Supabase")
                        authRepository.signInWithGoogle(idToken, nonce = null)
                        Log.d(TAG, "Supabase signInWithGoogle OK")
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue with Google")
            }
        } else {
            Log.d(TAG, "Google button hidden — enabled=$googleSignInEnabled webClientId=${googleWebClientId?.take(8)}… activity=${activity != null}")
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun validateInviteRequest(email: String, message: String): String? = when {
    email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email address."
    message.length > MESSAGE_LIMIT -> "Message must be 500 characters or fewer."
    else -> null
}

/// Launches Credential Manager to retrieve a Google ID token.
/// Returns null when the user dismisses the sheet, account reauth is cancelled,
/// or no Google account is available on the device. Credential Manager reports
/// those cases as `GetCredentialCancellationException` / `NoCredentialException`,
/// so callers should treat them as benign no-ops rather than auth failures.
private suspend fun requestGoogleIdToken(activity: Activity, webClientId: String): String? {
    val manager = CredentialManager.create(activity)

    // Prefer the recommended returning-user flow first. With filtering enabled,
    // Credential Manager only offers accounts that have previously authorized
    // this app and can auto-select when there is exactly one eligible account.
    val returningUserOption = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(true)
        .setAutoSelectEnabled(true)
        .build()

    Log.d(TAG, "requestGoogleIdToken: trying authorized Google accounts")
    val returningUser = runCatching {
        manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(returningUserOption).build())
    }
    returningUser.getOrNull()?.let { response ->
        return extractIdToken(response.credential)
    }
    val returningUserError = returningUser.exceptionOrNull()
    when (returningUserError) {
        is GetCredentialCancellationException -> {
            Log.i(TAG, "requestGoogleIdToken: Google sign-in dismissed during authorized-account flow")
            return null
        }
        is NoCredentialException -> {
            Log.d(TAG, "requestGoogleIdToken: no authorized Google credential, falling back to Sign in with Google")
        }
        null -> Unit
        else -> {
            Log.e(TAG, "requestGoogleIdToken: unexpected authorized-account error", returningUserError)
            throw returningUserError
        }
    }

    // Button flow: allows choosing any Google account and handles add-account /
    // reauth cases. A reauth cancellation is still an expected cancellation.
    val interactiveOption = GetSignInWithGoogleOption.Builder(webClientId).build()
    Log.d(TAG, "requestGoogleIdToken: invoking Sign in with Google")
    return try {
        val response = manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(interactiveOption).build())
        extractIdToken(response.credential)
    } catch (e: GetCredentialCancellationException) {
        Log.i(TAG, "requestGoogleIdToken: Google sign-in dismissed or account reauth cancelled (${e.message})")
        null
    } catch (e: NoCredentialException) {
        Log.i(TAG, "requestGoogleIdToken: no Google credential available (${e.message})")
        null
    } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "requestGoogleIdToken: failed to parse id_token", e)
        null
    } catch (e: Exception) {
        Log.e(TAG, "requestGoogleIdToken: unexpected interactive error", e)
        throw e
    }
}

private fun extractIdToken(credential: androidx.credentials.Credential): String? {
    if (credential is androidx.credentials.CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        Log.d(TAG, "extractIdToken: got Google id_token credential")
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    Log.w(TAG, "extractIdToken: unexpected credential type=${credential::class.java.name}")
    return null
}
