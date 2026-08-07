package com.eplaytime.app.worker

import java.util.Calendar

object EngagementMessages {

    fun getMessageForTime(hour: Int): String {
        val messages = when (hour) {
            in 5..7 -> earlyMorningMessages // 5AM - 8AM
            in 8..11 -> morningProductivityMessages // 8AM - 12PM
            in 12..13 -> noonLunchMessages // 12PM - 2PM
            in 14..16 -> afternoonBoostMessages // 2PM - 5PM
            in 17..19 -> eveningRelaxMessages // 5PM - 8PM
            in 20..21 -> nightChillMessages // 8PM - 10PM
            in 22..23 -> lateNightMessages // 10PM - 3AM
            in 0..4 -> lateNightMessages // Continuation of 10PM - 3AM
            else -> premiumMessages // Fallback
        }
        
        return messages.random()
    }

    private val earlyMorningMessages = listOf(
        "Wake up. Press play. Conquer the day. 🎶",
        "Your morning soundtrack is ready.",
        "Rise & vibe. Music first, everything else later.",
        "Start fresh with a new sound.",
        "Let this track fuel your morning energy.",
        "Good morning. Your ears deserve something beautiful."
    )

    private val morningProductivityMessages = listOf(
        "Deep work needs deep sound. 🎧",
        "Focus mode activated. Tap to play.",
        "Let music sharpen your mind.",
        "Today’s rhythm is waiting for you.",
        "Build. Create. Repeat. With music.",
        "Productivity hits different with the right track."
    )

    private val noonLunchMessages = listOf(
        "Lunch break + good music = perfect combo.",
        "Pause the stress. Play the vibe.",
        "Midday refresh coming your way.",
        "Recharge with rhythm.",
        "Take five. Hit play.",
        "Let this track reset your energy."
    )

    private val afternoonBoostMessages = listOf(
        "Feeling slow? This one will wake you up. 🔥",
        "Power hour needs power music.",
        "Energy dropping? Not anymore.",
        "Stay sharp. Stay loud.",
        "Let this beat push you forward.",
        "One tap. Instant motivation."
    )

    private val eveningRelaxMessages = listOf(
        "Long day? Let music handle it.",
        "Sunset vibes are here. 🌆",
        "Relax. Breathe. Play.",
        "Evening mood unlocked.",
        "Time to unwind with a smooth track.",
        "Let the music slow the world down."
    )

    private val nightChillMessages = listOf(
        "Night feels better with headphones on.",
        "Quiet moments. Perfect sound.",
        "Let tonight’s vibe begin. 🌙",
        "Press play and disappear for a while.",
        "Soft beats. Calm mind.",
        "Your night soundtrack is waiting."
    )

    private val lateNightMessages = listOf(
        "Still awake? Let music keep you company.",
        "Night owl mode activated.",
        "Late hours. Deep sounds.",
        "Some tracks hit different after midnight.",
        "Just you and the rhythm.",
        "Midnight focus playlist unlocked."
    )

    private val premiumMessages = listOf(
        "Your music, your moment.",
        "Tap and continue your vibe.",
        "Discover something you already love.",
        "Every play writes your story.",
        "Open Audia and press play.",
        "Music knows what you need.",
        "Your next session is waiting."
    )
    
    private val softEmotionalMessages = listOf(
        "Let the music say what you can’t.",
        "Some songs understand you.",
        "Feel it. Don’t skip it.",
        "Close your eyes. Press play.",
        "The right track at the right moment."
    )
}
