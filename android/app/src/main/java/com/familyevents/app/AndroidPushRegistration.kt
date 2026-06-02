package com.familyevents.app

import android.content.Context
import com.familyevents.core.UserId
import com.familyevents.data.PushPlatform
import com.familyevents.data.PushRegistrationRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

private fun firebaseConfigAvailable(): Boolean =
    BuildConfig.ANDROID_FIREBASE_APPLICATION_ID.isNotBlank() &&
        BuildConfig.ANDROID_FIREBASE_API_KEY.isNotBlank() &&
        BuildConfig.ANDROID_FIREBASE_PROJECT_ID.isNotBlank() &&
        BuildConfig.ANDROID_FIREBASE_GCM_SENDER_ID.isNotBlank()

fun ensureFirebaseInitialized(context: Context): Boolean {
    if (FirebaseApp.getApps(context).isNotEmpty()) return true
    if (!firebaseConfigAvailable()) return false

    val options = FirebaseOptions.Builder()
        .setApplicationId(BuildConfig.ANDROID_FIREBASE_APPLICATION_ID)
        .setApiKey(BuildConfig.ANDROID_FIREBASE_API_KEY)
        .setProjectId(BuildConfig.ANDROID_FIREBASE_PROJECT_ID)
        .setGcmSenderId(BuildConfig.ANDROID_FIREBASE_GCM_SENDER_ID)
        .build()

    FirebaseApp.initializeApp(context.applicationContext, options)
    return true
}

suspend fun registerAndroidPushToken(
    context: Context,
    userId: UserId,
    repository: PushRegistrationRepository,
) {
    if (!ensureFirebaseInitialized(context)) return
    val token = FirebaseMessaging.getInstance().token.await()
    if (token.isNotBlank()) {
        repository.registerMobilePushToken(userId, PushPlatform.Android, token)
    }
}
