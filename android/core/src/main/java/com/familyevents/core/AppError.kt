package com.familyevents.core

sealed class AppError(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    data class Network(val source: Throwable) : AppError("Network problem. Please try again.", source)
    data object Unauthorized : AppError("You're signed out. Please sign in again.")
    data object NotFound : AppError("We couldn't find that.")
    data class Config(val key: String) : AppError("Configuration error: $key is missing.")
    data object InvalidCredentials : AppError("Email or password is incorrect.")
    data object EmailAlreadyInUse : AppError("An account with that email already exists.")
    data object EmailNotConfirmed : AppError("Please confirm your email before signing in.")
    data class WeakPassword(val reason: String) : AppError(reason.ifBlank { "Choose a stronger password." })
    data object PasswordResetEmailSent : AppError("Check your email for a reset link.")
    data class SecureStorage(val source: Throwable) : AppError("Couldn't securely store your session. Please try again.", source)
    data class Unknown(val source: Throwable) : AppError("Something went wrong.", source)
}
