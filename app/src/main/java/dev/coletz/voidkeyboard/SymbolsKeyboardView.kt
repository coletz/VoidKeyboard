package dev.coletz.voidkeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.R as MaterialR
import kotlin.math.roundToInt

/** On-screen symbols keyboard toggled by the SYM key. [context] must be a [MaterialUi.themedContext]. */
@SuppressLint("ViewConstructor")
class SymbolsKeyboardView(
    context: Context,
    private val onSymbol: (Char) -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(MaterialUi.color(context, MaterialR.attr.colorSurfaceContainerHigh))

        val gap = MaterialUi.dp(context, KEY_GAP_DP)
        val padding = MaterialUi.dp(context, PADDING_DP)
        setPadding(padding, padding, padding, padding + gap)

        // Each row fills the whole width: keys are equally wide within a row, rows with fewer keys get wider keys
        val keyHeight = keyHeightPx()
        ROWS.forEach { row ->
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
            }
            row.forEach { symbol ->
                val key = SymbolKeyView(context, symbol).apply {
                    setOnClickListener { onSymbol(symbol) }
                }
                rowLayout.addView(key, LayoutParams(0, keyHeight, 1f).apply {
                    setMargins(gap / 2, gap / 2, gap / 2, gap / 2)
                })
            }
            addView(rowLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    // Scales with the screen: the BlackBerry KEYone display is short and almost square
    private fun keyHeightPx(): Int {
        val screenHeightDp = resources.configuration.screenHeightDp
        val heightDp = (screenHeightDp * KEY_HEIGHT_SCREEN_RATIO).roundToInt().coerceIn(MIN_KEY_HEIGHT_DP, MAX_KEY_HEIGHT_DP)
        return MaterialUi.dp(context, heightDp)
    }

    private companion object {
        val ROWS = listOf(
            "1234567890",
            "~@#$%&*-+()",
            "`^!\"':;/?,.",
            "<>[]{}_=|\\",
        )
        const val KEY_HEIGHT_SCREEN_RATIO = 0.075f
        const val MIN_KEY_HEIGHT_DP = 40
        const val MAX_KEY_HEIGHT_DP = 52
        const val KEY_GAP_DP = 6
        const val PADDING_DP = 4
    }
}

/**
 * A rounded tonal key. With effects enabled, pressing it squishes it into a pill and releasing it
 * springs back with a small overshoot (Material 3 Expressive shape morph).
 */
@SuppressLint("ViewConstructor")
private class SymbolKeyView(context: Context, symbol: Char) : AppCompatTextView(context) {

    private val restRadius = MaterialUi.dp(context, KEY_CORNER_DP)
    private val shape = MaterialUi.roundedRect(MaterialUi.color(context, MaterialR.attr.colorSurfaceBright), restRadius)
    private var pressAnimation: SpringAnimation? = null

    /** 0 = at rest, 1 = fully pressed. It briefly goes below 0 while springing back. */
    private var pressProgress = 0f
        set(value) {
            field = value
            val scale = 1f - PRESSED_SCALE_DELTA * value
            scaleX = scale
            scaleY = scale
            val pillRadius = minOf(width, height) / 2f
            shape.cornerRadius = (restRadius + (pillRadius - restRadius) * value).coerceAtLeast(0f)
        }

    init {
        text = symbol.toString()
        contentDescription = symbol.toString()
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextAppearance(MaterialUi.resolveTextAppearance(context, MaterialR.attr.textAppearanceTitleLarge))

        val onSurface = MaterialUi.color(context, MaterialR.attr.colorOnSurface)
        if (MaterialUi.effectsEnabled) {
            setTextColor(onSurface)
            pressAnimation = SpringAnimation(this, PRESS_PROGRESS).apply {
                spring = SpringForce()
                minimumVisibleChange = SpringAnimation.MIN_VISIBLE_CHANGE_SCALE
            }
        } else {
            // A single high contrast swap, readable on grayscale e-ink and with no intermediate frames
            val onPrimary = MaterialUi.color(context, MaterialR.attr.colorOnPrimary)
            setTextColor(
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()),
                    intArrayOf(onPrimary, onSurface)
                )
            )
        }
        background = MaterialUi.pressableBackground(context, shape) {
            MaterialUi.roundedRect(MaterialUi.color(context, androidx.appcompat.R.attr.colorPrimary), restRadius)
        }
    }

    override fun setPressed(pressed: Boolean) {
        val changed = pressed != isPressed
        super.setPressed(pressed)
        if (!changed) return
        if (pressed) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        pressAnimation?.apply {
            spring.stiffness = if (pressed) PRESS_STIFFNESS else RELEASE_STIFFNESS
            spring.dampingRatio = if (pressed) SpringForce.DAMPING_RATIO_NO_BOUNCY else RELEASE_DAMPING
            animateToFinalPosition(if (pressed) 1f else 0f)
        }
    }

    private companion object {
        const val KEY_CORNER_DP = 12f
        const val PRESSED_SCALE_DELTA = 0.08f
        const val PRESS_STIFFNESS = 1500f
        const val RELEASE_STIFFNESS = 600f
        const val RELEASE_DAMPING = 0.5f

        val PRESS_PROGRESS = object : FloatPropertyCompat<SymbolKeyView>("pressProgress") {
            override fun getValue(view: SymbolKeyView) = view.pressProgress
            override fun setValue(view: SymbolKeyView, value: Float) {
                view.pressProgress = value
            }
        }
    }
}
