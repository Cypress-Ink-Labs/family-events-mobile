package com.familyevents.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    googleSignInEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
            onClick = { scope.launch { authRepository.signIn(email, password) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in")
        }
        OutlinedButton(
            onClick = { scope.launch { authRepository.signUp(email, password) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create account")
        }
        OutlinedButton(
            onClick = { scope.launch { authRepository.resetPassword(email) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset password")
        }
        if (googleSignInEnabled) {
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Continue with Google")
            }
        }
    }
}
