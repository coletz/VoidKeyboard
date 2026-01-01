package dev.coletz.voidkeyboard

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class AlternativesDialog(
    private val context: Context,
    private val alternatives: List<Char>,
    private val cursorX: Int?,
    private val onCharacterSelected: (Char) -> Unit
) {

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

        val scrollView = HorizontalScrollView(overlayContext).apply {
            isHorizontalScrollBarEnabled = false
            background = borderDrawable
            setPadding(8, 4, 8, 4)
        }

        val container = LinearLayout(overlayContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        alternatives.forEach { char ->
            val textView = TextView(overlayContext).apply {
                text = char.toString()
                textSize = 20f
                setPadding(12, 0, 12, 0)
                setOnClickListener {
                    onCharacterSelected(char)
                    dismiss()
                }
            }
            container.addView(textView)
        }

        scrollView.addView(container)
        dialog.setContentView(scrollView)

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

            if (cursorX != null) {
                setGravity(Gravity.BOTTOM or Gravity.LEFT)
                attributes = attributes.apply {
                    x = cursorX
                    y = 48
                }
            } else {
                setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                attributes = attributes.apply {
                    y = 48
                }
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
}
