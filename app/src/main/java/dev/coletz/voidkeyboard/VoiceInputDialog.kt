package dev.coletz.voidkeyboard

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.R as MaterialR

class VoiceInputDialog(private val context: Context) {

    private var dialog: Dialog? = null
    private var windowContext: Context? = null
    private var haloScaleX: SpringAnimation? = null
    private var haloScaleY: SpringAnimation? = null

    fun show() {
        if (!OverlayPermissionHelper.hasPermission(context)) {
            OverlayPermissionHelper.requestPermission(context)
            return
        }

        dismiss()

        val overlayContext = OverlayPermissionHelper.createOverlayContext(context)
        windowContext = overlayContext

        val dialog = Dialog(overlayContext, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
        dialog.setContentView(createContent(MaterialUi.themedContext(overlayContext)))

        dialog.window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            decorView.setPadding(0, 0, 0, 0)
            if (!MaterialUi.effectsEnabled) setWindowAnimations(0)
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            attributes = attributes.apply {
                y = popupOffsetY()
            }
        }

        dialog.setOnDismissListener {
            this.dialog = null
        }

        dialog.show()
        this.dialog = dialog
    }

    /** Makes the halo behind the microphone follow the voice level. Ignored when effects are disabled. */
    fun onRmsChanged(rmsdB: Float) {
        // SpeechRecognizer reports roughly -2 dB (silence) to 10 dB (loud speech)
        val level = ((rmsdB - RMS_MIN_DB) / (RMS_MAX_DB - RMS_MIN_DB)).coerceIn(0f, 1f)
        val scale = 1f + (MAX_HALO_SCALE - 1f) * level
        haloScaleX?.animateToFinalPosition(scale)
        haloScaleY?.animateToFinalPosition(scale)
    }

    private fun createContent(context: Context): FrameLayout {
        val halo = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(MaterialUi.color(context, MaterialR.attr.colorPrimaryContainer))
            }
        }
        if (MaterialUi.effectsEnabled) {
            haloScaleX = createHaloSpring(halo, DynamicAnimation.SCALE_X)
            haloScaleY = createHaloSpring(halo, DynamicAnimation.SCALE_Y)
        }

        val micIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(MaterialUi.color(context, MaterialR.attr.colorOnPrimaryContainer))
        }

        val haloSize = MaterialUi.dp(context, HALO_SIZE_DP)
        val iconSize = MaterialUi.dp(context, ICON_SIZE_DP)
        val micContainer = FrameLayout(context).apply {
            clipChildren = false
            addView(halo, FrameLayout.LayoutParams(haloSize, haloSize, Gravity.CENTER))
            addView(micIcon, FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER))
        }
        // Sized for the largest halo, so the popup doesn't change size while it pulses
        val micContainerSize = (haloSize * MAX_HALO_SCALE).toInt()

        val label = TextView(context).apply {
            setText(R.string.voice_listening)
            setTextAppearance(MaterialUi.resolveTextAppearance(context, MaterialR.attr.textAppearanceTitleMedium))
            setTextColor(MaterialUi.color(context, MaterialR.attr.colorOnSurface))
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            background = MaterialUi.popupSurface(context)
            if (MaterialUi.effectsEnabled) elevation = MaterialUi.dp(context, MaterialUi.POPUP_ELEVATION_DP)
            val padding = MaterialUi.dp(context, CARD_PADDING_DP)
            setPadding(padding, padding, padding * 3, padding)
            addView(micContainer, LinearLayout.LayoutParams(micContainerSize, micContainerSize))
            addView(label, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = padding * 2 })
        }

        // Room for the card shadow, the dialog window itself is transparent
        return FrameLayout(context).apply {
            val shadowSpace = MaterialUi.dp(context, SHADOW_SPACE_DP)
            setPadding(shadowSpace, shadowSpace, shadowSpace, shadowSpace)
            clipToPadding = false
            addView(card)
        }
    }

    private fun createHaloSpring(halo: View, property: DynamicAnimation.ViewProperty) =
        SpringAnimation(halo, property, 1f).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }

    fun dismiss() {
        haloScaleX?.cancel()
        haloScaleY?.cancel()
        haloScaleX = null
        haloScaleY = null
        dialog?.dismiss()
        dialog = null
        windowContext = null
    }

    fun isShowing(): Boolean = dialog?.isShowing == true

    private fun popupOffsetY(): Int = MaterialUi.dp(context, POPUP_OFFSET_Y_DP - SHADOW_SPACE_DP)

    private companion object {
        const val POPUP_OFFSET_Y_DP = 32
        const val SHADOW_SPACE_DP = 12
        const val CARD_PADDING_DP = 8
        const val HALO_SIZE_DP = 44
        const val ICON_SIZE_DP = 24
        const val MAX_HALO_SCALE = 1.3f
        const val RMS_MIN_DB = -2f
        const val RMS_MAX_DB = 10f
    }
}
