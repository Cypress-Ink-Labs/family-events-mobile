package com.familyevents.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.familyevents.core.AppError
import com.familyevents.core.CityId
import com.familyevents.core.UserId
import com.familyevents.data.AuthRepository
import com.familyevents.data.CityRepository
import com.familyevents.data.ProfileRepository
import com.familyevents.data.UserProfileUpdate
import com.familyevents.designsystem.AppThemePreference
import kotlinx.coroutines.launch

@Composable
fun ProfileDialog(
    userId: UserId,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    cityRepository: CityRepository,
    themePreference: AppThemePreference,
    onThemePreferenceChange: (AppThemePreference) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val cities by cityRepository.observeCities().collectAsStateWithLifecycle(initialValue = emptyList())
    var loading by remember(userId) { mutableStateOf(true) }
    var saving by remember(userId) { mutableStateOf(false) }
    var errorMessage by remember(userId) { mutableStateOf<String?>(null) }
    var email by remember(userId) { mutableStateOf<String?>(null) }
    var displayName by remember(userId) { mutableStateOf("") }
    var childName by remember(userId) { mutableStateOf("") }
    var childAge by remember(userId) { mutableStateOf("") }
    var selectedCityId by remember(userId) { mutableStateOf<CityId?>(null) }
    var currentPassword by remember(userId) { mutableStateOf("") }
    var newPassword by remember(userId) { mutableStateOf("") }
    var confirmPassword by remember(userId) { mutableStateOf("") }
    var passwordMessage by remember(userId) { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        loading = true
        errorMessage = null
        runCatching {
            cityRepository.refreshCities()
            profileRepository.profile(userId)
        }.onSuccess { profile ->
            email = profile.email
            displayName = profile.displayName.orEmpty()
            childName = profile.childName.orEmpty()
            childAge = profile.childAge?.toString().orEmpty()
            selectedCityId = profile.currentCityId
        }.onFailure { error ->
            errorMessage = error.toUserMessage()
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Profile") },
        text = {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Loading profile")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AccountSummary(displayName, email ?: userId.rawValue)
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it },
                        label = { Text("Child name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = childAge,
                        onValueChange = { childAge = it.filter(Char::isDigit).take(2) },
                        label = { Text("Child age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Preferred city", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cities.forEach { city ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                RadioButton(
                                    selected = selectedCityId == city.id,
                                    onClick = { selectedCityId = city.id },
                                )
                                Text("${city.name}${city.region?.let { ", $it" }.orEmpty()}")
                            }
                        }
                    }
                    Text("Appearance", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemePreference.entries.forEach { preference ->
                            FilterChip(
                                selected = themePreference == preference,
                                onClick = { onThemePreferenceChange(preference) },
                                label = { Text(preference.label) },
                            )
                        }
                    }
                    HorizontalDivider()
                    Text("Password", style = MaterialTheme.typography.labelLarge)
                    passwordMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                passwordMessage = validatePassword(email, currentPassword, newPassword, confirmPassword)
                                if (passwordMessage == null) {
                                    runCatching {
                                        authRepository.changePassword(email.orEmpty(), currentPassword, newPassword)
                                    }.onSuccess {
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                        passwordMessage = "Password updated."
                                    }.onFailure { passwordMessage = it.toUserMessage() }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Change password") }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { scope.launch { authRepository.signOut() } }) { Text("Sign out") }
                        TextButton(onClick = { scope.launch { profileRepository.deleteAccount(userId); authRepository.signOut() } }) {
                            Text("Delete account", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        errorMessage = validateAge(childAge)
                        if (errorMessage == null) {
                            runCatching {
                                profileRepository.updateProfile(
                                    userId,
                                    UserProfileUpdate(
                                        displayName = displayName.trimToNull(),
                                        currentCityId = selectedCityId,
                                        childName = childName.trimToNull(),
                                        childAge = childAge.trimToNull()?.toInt(),
                                    ),
                                )
                            }.onFailure { errorMessage = it.toUserMessage() }
                        }
                        saving = false
                    }
                },
            ) { Text(if (saving) "Saving" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } },
    )
}

@Composable
private fun AccountSummary(displayName: String, fallback: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(displayName.trimToNull() ?: fallback, style = MaterialTheme.typography.titleMedium)
        Text(fallback, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
    }
}

private fun validateAge(rawAge: String): String? {
    val text = rawAge.trim()
    if (text.isEmpty()) return null
    val age = text.toIntOrNull() ?: return "Child age must be a number."
    return if (age in 0..18) null else "Child age must be between 0 and 18."
}

private fun validatePassword(email: String?, currentPassword: String, newPassword: String, confirmPassword: String): String? = when {
    email.isNullOrBlank() -> "Profile email is required to change password."
    currentPassword.isBlank() -> "Enter your current password."
    newPassword.length < 6 -> "New password must be at least 6 characters."
    newPassword != confirmPassword -> "New passwords do not match."
    newPassword == currentPassword -> "New password must be different."
    else -> null
}

private fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }

private fun Throwable.toUserMessage(): String = when (this) {
    is AppError -> message ?: "Something went wrong."
    else -> message ?: "Something went wrong."
}
