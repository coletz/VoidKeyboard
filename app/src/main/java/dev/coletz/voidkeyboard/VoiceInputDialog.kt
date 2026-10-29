package dev.coletz.voidkeyboard

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout

class VoiceInputDialog(private val context: Context) {

    private var dialog: Dialog? = null
    private var windowContext: Context? = null

    fun show() {
        if (!OverlayPermissionHelper.hasPermission(context)) {
            OverlayPermissionHelper.requestPermission(context)
            return
        }

        dismiss()

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(0)

        windowContext = context.createWindowContext(
            display,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            null
        )

        val overlayContext = windowContext ?: return

        val dialog = Dialog(overlayContext, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)

        val borderDrawable = GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke(1, Color.BLACK)
            cornerRadius = 16f
        }

        val container = LinearLayout(overlayContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = borderDrawable
            setPadding(24, 16, 24, 16)
        }

        val micIcon = ImageView(overlayContext).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setColorFilter(Color.BLACK)
        }

        container.addView(micIcon)
        dialog.setContentView(container)

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

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        windowContext = null
    }

    fun isShowing(): Boolean = dialog?.isShowing == true

    private fun popupOffsetY(): Int = (POPUP_OFFSET_Y_DP * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val POPUP_OFFSET_Y_DP = 32
    }
}
