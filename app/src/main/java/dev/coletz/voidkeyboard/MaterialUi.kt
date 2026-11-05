package dev.coletz.voidkeyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MaterialR

/** Material 3 helpers for the views built in code (symbols keyboard and overlay popups) */
object MaterialUi {

    /**
     * Ripples, press animations, popup transitions and shadows. Turned off per flavor in
     * build.gradle.kts, because on e-ink screens every intermediate frame is a visible refresh.
     */
    val effectsEnabled: Boolean get() = BuildConfig.UI_EFFECTS

    /** Services and window contexts are not themed with Material 3, so views built from them need this */
    fun themedContext(base: Context): Context =
        DynamicColors.wrapContextIfAvailable(ContextThemeWrapper(base, R.style.Theme_VoidKeyboard_Input))

    fun color(context: Context, @AttrRes attr: Int): Int =
        MaterialColors.getColor(context, attr, MaterialUi::class.java.simpleName)

    @StyleRes
    fun resolveTextAppearance(context: Context, @AttrRes attr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return value.resourceId
    }

    fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density

    fun dp(context: Context, value: Int): Int = dp(context, value.toFloat()).toInt()

    fun roundedRect(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    /** Popup card: a shadow with effects, a static outline without (shadows look dithered on e-ink) */
    fun popupSurface(context: Context): GradientDrawable =
        roundedRect(color(context, MaterialR.attr.colorSurfaceContainerHigh), dp(context, POPUP_CORNER_DP)).apply {
            if (!effectsEnabled) {
                setStroke(dp(context, 1), color(context, MaterialR.attr.colorOutline))
            }
        }

    /** Background for a tappable element: a ripple with effects, an instant pressed color without */
    fun pressableBackground(context: Context, content: Drawable?, pressedShape: () -> Drawable): Drawable =
        if (effectsEnabled) {
            RippleDrawable(
                ColorStateList.valueOf(color(context, android.R.attr.colorControlHighlight)),
                content,
                if (content == null) pressedShape() else null
            )
        } else {
            StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), pressedShape())
                content?.let { addState(intArrayOf(), it) }
            }
        }

    const val POPUP_CORNER_DP = 20f
    const val POPUP_ELEVATION_DP = 6f
}
