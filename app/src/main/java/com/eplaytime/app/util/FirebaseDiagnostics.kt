package com.eplaytime.app.util

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.eplaytime.app.BuildConfig
import com.eplaytime.app.data.datastore.PlayTimeDataStore
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * FirebaseDiagnostics - Central privacy-first diagnostics & telemetry manager for Audia Player.
 *
 * Provides:
 * 1. Breadcrumbs & Crash Context: Records which screen, what action, playback state, and session duration.
 * 2. Privacy Invariants: NEVER logs song titles, artist names, personal file paths, listening histories,
 *    or precise location.
 * 3. Separate controls: Analytics and Crashlytics can be independently enabled/disabled.
 */
object FirebaseDiagnostics {

    private const val TAG = "FirebaseDiagnostics"

    private val sessionStartTimeWall: Long = System.currentTimeMillis()
    private val sessionStartTimeElapsed: Long = SystemClock.elapsedRealtime()

    @Volatile
    private var isAnalyticsActive: Boolean = true

    @Volatile
    private var isCrashlyticsActive: Boolean = true

    @Volatile
    private var currentScreenName: String = "AppStart"

    @Volatile
    private var lastRecordedAction: String = "app_launch"

    @Volatile
    private var lastRecordedActionDetail: String = ""

    @Volatile
    private var currentPlaybackState: String = "IDLE"

    @Volatile
    private var currentAudioRoute: String = "UNKNOWN"

    private val DISALLOWED_PARAM_KEYWORDS = listOf(
        "song", "title", "artist", "album", "path", "file", "uri", "track",
        "folder", "dir", "location", "lat", "lon", "query", "search", "name",
        "user", "email", "phone"
    )

    fun init(context: Context, dataStore: PlayTimeDataStore, scope: CoroutineScope) {
        // Initial setup for Crashlytics custom keys
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("app_name", "Audia Player")
            crashlytics.setCustomKey("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            crashlytics.setCustomKey("current_screen", "AppStart")
            crashlytics.setCustomKey("last_action", "app_launch")
            crashlytics.setCustomKey("session_start_time", sessionStartTimeWall)
            crashlytics.setCustomKey("session_duration", "0s")
            crashlytics.setCustomKey("session_duration_seconds", 0L)
            crashlytics.setCustomKey("playback_state", "IDLE")
            crashlytics.setCustomKey("audio_route", "UNKNOWN")
            crashlytics.log("Audia Application launched [Build ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})]")
        }

        // Install UncaughtExceptionHandler to capture up-to-the-second duration, screen, and last action on ANY crash
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val crashlytics = FirebaseCrashlytics.getInstance()
                val duration = getSessionDurationFormatted()
                val durationSec = getSessionDurationSeconds()
                crashlytics.setCustomKey("current_screen", currentScreenName)
                crashlytics.setCustomKey("last_action", lastRecordedAction)
                if (lastRecordedActionDetail.isNotEmpty()) {
                    crashlytics.setCustomKey("last_action_detail", lastRecordedActionDetail)
                }
                crashlytics.setCustomKey("session_duration", duration)
                crashlytics.setCustomKey("session_duration_seconds", durationSec)
                crashlytics.setCustomKey("playback_state", currentPlaybackState)
                crashlytics.setCustomKey("audio_route", currentAudioRoute)
                crashlytics.log("CRASH on [${thread.name}] at Screen: '$currentScreenName' after $duration. Last action: '$lastRecordedAction'. ${throwable.javaClass.simpleName}: ${throwable.message}")
            }
            previousHandler?.uncaughtException(thread, throwable)
        }

        scope.launch(Dispatchers.IO) {
            dataStore.isAnalyticsEnabled.collectLatest { enabled ->
                isAnalyticsActive = enabled
                runCatching {
                    FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
                    Log.d(TAG, "Firebase Analytics collection set to: $enabled")
                }.onFailure { Log.w(TAG, "Failed setting Analytics collection state", it) }
            }
        }

        scope.launch(Dispatchers.IO) {
            dataStore.isCrashlyticsEnabled.collectLatest { enabled ->
                isCrashlyticsActive = enabled
                runCatching {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
                    Log.d(TAG, "Firebase Crashlytics collection set to: $enabled")
                }.onFailure { Log.w(TAG, "Failed setting Crashlytics collection state", it) }
            }
        }
    }

    /**
     * Formats elapsed session time (e.g. "3m 45s" or "25s").
     */
    fun getSessionDurationFormatted(): String {
        val totalSec = (SystemClock.elapsedRealtime() - sessionStartTimeElapsed) / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun getSessionDurationSeconds(): Long {
        return (SystemClock.elapsedRealtime() - sessionStartTimeElapsed) / 1000
    }

    /**
     * Record UI screen navigation (Breadcrumbs + Crashlytics Custom Key + Analytics screen_view).
     */
    fun recordScreen(context: Context, screenName: String) {
        val sanitized = sanitizeIdentifier(screenName)
        currentScreenName = sanitized

        // Update Crashlytics context
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("current_screen", sanitized)
            crashlytics.setCustomKey("session_duration", getSessionDurationFormatted())
            crashlytics.setCustomKey("session_duration_seconds", getSessionDurationSeconds())
            crashlytics.log("Screen: $sanitized (at ${getSessionDurationFormatted()})")
        }

        // Update Analytics if active
        if (isAnalyticsActive) {
            runCatching {
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, sanitized)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, sanitized)
                }
                FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
                Log.d(TAG, "Screen view logged: $sanitized")
            }
        }
    }

    /**
     * Record user interaction / event (Breadcrumbs + Crashlytics Custom Key + Analytics event).
     */
    fun recordAction(context: Context, action: String, detail: String = "", params: Map<String, Any> = emptyMap()) {
        val safeAction = sanitizeIdentifier(action)
        val safeDetail = if (detail.isNotEmpty()) sanitizeIdentifier(detail) else ""
        lastRecordedAction = safeAction
        lastRecordedActionDetail = safeDetail

        val durationFormatted = getSessionDurationFormatted()
        val durationSec = getSessionDurationSeconds()

        // 1. Crashlytics Breadcrumb & Keys
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("last_action", safeAction)
            if (safeDetail.isNotEmpty()) {
                crashlytics.setCustomKey("last_action_detail", safeDetail)
            }
            crashlytics.setCustomKey("session_duration", durationFormatted)
            crashlytics.setCustomKey("session_duration_seconds", durationSec)
            crashlytics.log("Action [$durationFormatted]: $safeAction ${if (safeDetail.isNotEmpty()) "($safeDetail)" else ""}")
        }

        // 2. Analytics Event
        if (isAnalyticsActive) {
            val bundle = Bundle().apply {
                putString("action_name", safeAction)
                if (safeDetail.isNotEmpty()) {
                    putString("action_detail", safeDetail)
                }
                putLong("session_elapsed_sec", durationSec)
            }

            params.forEach { (key, value) ->
                val lowerKey = key.lowercase(Locale.ROOT)
                val isDisallowed = DISALLOWED_PARAM_KEYWORDS.any { lowerKey.contains(it) }
                if (!isDisallowed) {
                    when (value) {
                        is String -> bundle.putString(lowerKey, sanitizeIdentifier(value))
                        is Int -> bundle.putInt(lowerKey, value)
                        is Long -> bundle.putLong(lowerKey, value)
                        is Float -> bundle.putFloat(lowerKey, value)
                        is Double -> bundle.putDouble(lowerKey, value)
                        is Boolean -> bundle.putBoolean(lowerKey, value)
                    }
                }
            }

            runCatching {
                FirebaseAnalytics.getInstance(context).logEvent(safeAction, bundle)
                Log.d(TAG, "Feature action logged: $safeAction")
            }
        }
    }

    /**
     * Record playback state changes (PLAYING, PAUSED, BUFFERING, STOPPED).
     */
    fun recordPlaybackState(state: String) {
        val sanitized = sanitizeIdentifier(state)
        currentPlaybackState = sanitized
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("playback_state", sanitized)
            crashlytics.log("PlaybackState: $sanitized (at ${getSessionDurationFormatted()})")
        }
    }

    /**
     * Record audio output route changes (SPEAKER, BLUETOOTH, HEADPHONES).
     */
    fun recordAudioRoute(route: String) {
        val sanitized = sanitizeIdentifier(route)
        currentAudioRoute = sanitized
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("audio_route", sanitized)
            crashlytics.log("AudioRoute: $sanitized (at ${getSessionDurationFormatted()})")
        }
    }

    /**
     * Log non-fatal caught exceptions with rich session context.
     */
    fun recordNonFatal(throwable: Throwable, tag: String = "") {
        if (!isCrashlyticsActive) return
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("session_duration", getSessionDurationFormatted())
            crashlytics.setCustomKey("session_duration_seconds", getSessionDurationSeconds())
            if (tag.isNotBlank()) {
                crashlytics.setCustomKey("error_tag", sanitizeIdentifier(tag))
            }
            crashlytics.recordException(throwable)
        }
    }

    /**
     * Safe debug-only validation for Analytics.
     */
    fun sendTestDiagnosticEvent(context: Context) {
        runCatching {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
            val bundle = Bundle().apply {
                putString("diagnostic_type", "debug_manual_test")
                putString("screen_at_test", currentScreenName)
                putString("session_duration", getSessionDurationFormatted())
                putLong("timestamp", System.currentTimeMillis())
            }
            FirebaseAnalytics.getInstance(context).logEvent("audia_diagnostic_test", bundle)
            recordAction(context, "test_event_dispatched", "SettingsDialog")
            Log.d(TAG, "Diagnostic test event dispatched successfully")
        }
    }

    /**
     * Safe debug-only validation for Crashlytics.
     * Updates all custom keys before crashing so Crashlytics shows the full user journey!
     */
    fun triggerTestCrash(context: Context? = null) {
        val durationFormatted = getSessionDurationFormatted()
        val durationSec = getSessionDurationSeconds()
        lastRecordedAction = "test_crash_button_tap"
        lastRecordedActionDetail = "SettingsDialog"

        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("current_screen", currentScreenName)
            crashlytics.setCustomKey("last_action", "test_crash_button_tap")
            crashlytics.setCustomKey("last_action_detail", "SettingsDialog")
            crashlytics.setCustomKey("session_duration", durationFormatted)
            crashlytics.setCustomKey("session_duration_seconds", durationSec)
            crashlytics.setCustomKey("playback_state", currentPlaybackState)
            crashlytics.setCustomKey("audio_route", currentAudioRoute)
            crashlytics.log("CRASH TRIGGERED MANUALLY from $currentScreenName after $durationFormatted with last action 'test_crash_button_tap'")
        }

        if (BuildConfig.DEBUG) {
            throw RuntimeException("Audia Debug Verification Test Crash (Screen: $currentScreenName, Duration: $durationFormatted)")
        } else {
            recordNonFatal(RuntimeException("Audia Production Test Diagnostic Non-Fatal"), "manual_test")
        }
    }

    private fun sanitizeIdentifier(input: String): String {
        return input.trim()
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9_]".toRegex(), "_")
            .take(40)
    }
}
