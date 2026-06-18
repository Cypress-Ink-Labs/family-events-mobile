package com.familyevents.data

import android.util.Log

internal const val RepoLogTag = "FEData"

/** Runs [block]; on failure logs at WARN and returns null. Preserves the
 *  existing fallback semantics (caller treats null as "use fallback"). The
 *  logging call is itself guarded so a logging-framework failure (e.g. the
 *  unmocked `android.util.Log` stub in JVM unit tests) never changes the
 *  fallback behavior. */
internal inline fun <T> repoOrNull(operation: String, block: () -> T): T? =
    runCatching(block).getOrElse { error ->
        runCatching { Log.w(RepoLogTag, "API call failed: $operation — falling back", error) }
        null
    }
