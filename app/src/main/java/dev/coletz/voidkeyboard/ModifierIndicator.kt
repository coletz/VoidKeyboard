package dev.coletz.voidkeyboard

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.R as MaterialR

/**
 * Small overlay chip that shows which modifiers are locked (caps lock, ALT lock).
 * The window is neither focusable nor touchable, so it never steals input from the app below.
 */
class ModifierIndicator(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var label: TextView? = null
    private var currentText: String? = null

    /** Shows the given labels, or removes the overlay when the list is empty */
    fun update(labels: List<String>) {
        if (labels.isEmpty()) {
            dismiss()
            return
        }

        val text = labels.joinToString(SEPARATOR)
        if (text == currentText && rootView != null) return

        if (rootView == null && !show()) return
        label?.text = text
        currentText = text
    }

    private fun show(): Boolean {
        if (!OverlayPermissionHelper.hasPermission(context)) {
            OverlayPermissionHelper.requestPermission(context)
            return false
        }

        val overlayContext = OverlayPermissionHelper.createOverlayContext(context)
        val themedContext = MaterialUi.themedContext(overlayContext)
        val wm = overlayContext.getSystemService(WindowManager::class.java)

        val chip = TextView(themedContext).apply {
            setTextAppearance(MaterialUi.resolveTextAppearance(themedContext, MaterialR.attr.textAppearanceLabelLarge))
            setTextColor(MaterialUi.color(themedContext, MaterialR.attr.colorOnPrimaryContainer))
            background = MaterialUi.roundedRect(
                MaterialUi.color(themedContext, MaterialR.attr.colorPrimaryContainer),
                MaterialUi.dp(themedContext, CHIP_CORNER_DP)
            ).apply {
                // Without effects there is no shadow, so an outline keeps the chip readable on e-ink
                if (!MaterialUi.effectsEnabled) {
                    setStroke(MaterialUi.dp(themedContext, 1), MaterialUi.color(themedContext, MaterialR.attr.colorOutline))
                }
            }
            if (MaterialUi.effectsEnabled) elevation = MaterialUi.dp(themedContext, MaterialUi.POPUP_ELEVATION_DP)
            val horizontal = MaterialUi.dp(themedContext, CHIP_PADDING_H_DP)
            val vertical = MaterialUi.dp(themedContext, CHIP_PADDING_V_DP)
            setPadding(horizontal, vertical, horizontal, vertical)
        }

        // Room for the chip shadow, the window itself is transparent
        val root = FrameLayout(themedContext).apply {
            val shadowSpace = MaterialUi.dp(themedContext, SHADOW_SPACE_DP)
            setPadding(shadowSpace, shadowSpace, shadowSpace, shadowSpace)
            clipToPadding = false
            addView(chip)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = MaterialUi.dp(themedContext, MARGIN_DP - SHADOW_SPACE_DP)
            y = MaterialUi.dp(themedContext, OFFSET_Y_DP - SHADOW_SPACE_DP)
            if (MaterialUi.effectsEnabled) windowAnimations = android.R.style.Animation_Toast
        }

        return try {
            wm.addView(root, params)
            windowManager = wm
            rootView = root
            label = chip
            true
        } catch (e: RuntimeException) {
            "Unable to show the modifier indicator: $e".log()
            false
        }
    }

    fun dismiss() {
        rootView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: IllegalArgumentException) {
                "Modifier indicator was already removed: $e".log()
            }
        }
        rootView = null
        label = null
        windowManager = null
        currentText = null
    }

    private companion object {
        const val SEPARATOR = "  ·  "
        const val OFFSET_Y_DP = 48
        const val MARGIN_DP = 16
        const val SHADOW_SPACE_DP = 12
        const val CHIP_CORNER_DP = 12f
        const val CHIP_PADDING_H_DP = 12
        const val CHIP_PADDING_V_DP = 6
    }
}
