package com.blanksstudio.gridpix.ui.common

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptic and sound feedback honouring the Settings toggles. Sound uses the system
 * ToneGenerator so no audio assets are needed (SPEC section 7: sound-heavy polish is a non-goal).
 */
class Feedback internal constructor(
    private val haptics: () -> Boolean,
    private val sound: () -> Boolean,
    private val performHaptic: (HapticFeedbackType) -> Unit,
    private val tone: ToneGenerator?,
) {
    fun tap() {
        if (haptics()) performHaptic(HapticFeedbackType.TextHandleMove)
        if (sound()) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
    }

    fun longPress() {
        if (haptics()) performHaptic(HapticFeedbackType.LongPress)
    }

    fun win() {
        if (haptics()) performHaptic(HapticFeedbackType.LongPress)
        if (sound()) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
    }
}

@Composable
fun rememberFeedback(hapticsEnabled: Boolean, soundEnabled: Boolean): Feedback {
    val haptic = LocalHapticFeedback.current
    val hapticsState = rememberUpdatedState(hapticsEnabled)
    val soundState = rememberUpdatedState(soundEnabled)
    val tone = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 40) }.getOrNull()
    }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    return remember(haptic, tone) {
        Feedback(
            haptics = { hapticsState.value },
            sound = { soundState.value },
            performHaptic = { haptic.performHapticFeedback(it) },
            tone = tone,
        )
    }
}
