package dev.coletz.voidkeyboard

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.R as MaterialR

class AlternativesDialog(
    private val context: Context,
    private val alternatives: List<Char>,
    private val cursorX: Int?,
    private val onCharacterSelected: (Char) -> Unit
) {

    private var dialog: Dialog? = null
    private var windowContext: Context? = null

    // cursorX is an absolute screen coordinate, so LEFT (not START) is correct in RTL locales too
    @SuppressLint("RtlHardcoded")
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

            if (cursorX != null) {
                setGravity(Gravity.BOTTOM or Gravity.LEFT)
                attributes = attributes.apply {
                    // The card sits inside the shadow padding, so shift the window to keep it on the cursor
                    x = cursorX - shadowSpace()
                    y = popupOffsetY()
                }
            } else {
                setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                attributes = attributes.apply {
                    y = popupOffsetY()
                }
            }
        }

        dialog.setOnDismissListener {
            this.dialog = null
        }

        dialog.show()
        this.dialog = dialog
    }

    private fun createContent(context: Context): FrameLayout {
        val cardPadding = MaterialUi.dp(context, CARD_PADDING_DP)
        val card = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            background = MaterialUi.popupSurface(context)
            clipToOutline = true
            if (MaterialUi.effectsEnabled) elevation = MaterialUi.dp(context, MaterialUi.POPUP_ELEVATION_DP)
            setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        alternatives.forEachIndexed { index, char ->
            row.addView(createCell(context, char, index))
        }
        card.addView(row)

        // Room for the card shadow, the dialog window itself is transparent
        return FrameLayout(context).apply {
            val shadowSpace = shadowSpace()
            setPadding(shadowSpace, shadowSpace, shadowSpace, shadowSpace)
            clipToPadding = false
            addView(card)
        }
    }

    // ALT + digit picks one of the first nine alternatives, so the cell shows its digit under the char.
    // It must be the ASCII digit printed on the key, not a locale formatted number.
    @SuppressLint("SetTextI18n")
    private fun createCell(context: Context, char: Char, index: Int): LinearLayout {
        val cornerRadius = MaterialUi.dp(context, CELL_CORNER_DP)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumWidth = MaterialUi.dp(context, CELL_MIN_WIDTH_DP)
            val padding = MaterialUi.dp(context, CELL_PADDING_DP)
            setPadding(padding, padding, padding, padding)
            background = MaterialUi.pressableBackground(context, null) {
                MaterialUi.roundedRect(MaterialUi.color(context, MaterialR.attr.colorSecondaryContainer), cornerRadius)
            }
            setOnClickListener {
                onCharacterSelected(char)
                dismiss()
            }

            addView(TextView(context).apply {
                text = char.toString()
                setTextAppearance(MaterialUi.resolveTextAppearance(context, MaterialR.attr.textAppearanceTitleLarge))
                setTextColor(MaterialUi.color(context, MaterialR.attr.colorOnSurface))
            })
            if (index < MAX_SHORTCUTS) {
                addView(TextView(context).apply {
                    text = (index + 1).toString()
                    setTextAppearance(MaterialUi.resolveTextAppearance(context, MaterialR.attr.textAppearanceLabelSmall))
                    setTextColor(MaterialUi.color(context, MaterialR.attr.colorOnSurfaceVariant))
                })
            }
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        windowContext = null
    }

    fun isShowing(): Boolean = dialog?.isShowing == true

    private fun popupOffsetY(): Int = MaterialUi.dp(context, POPUP_OFFSET_Y_DP - SHADOW_SPACE_DP)

    private fun shadowSpace(): Int = MaterialUi.dp(context, SHADOW_SPACE_DP)

    private companion object {
        const val POPUP_OFFSET_Y_DP = 32
        const val SHADOW_SPACE_DP = 12
        const val CARD_PADDING_DP = 4
        const val CELL_PADDING_DP = 6
        const val CELL_MIN_WIDTH_DP = 44
        const val CELL_CORNER_DP = 14f
        const val MAX_SHORTCUTS = 9
    }
}
