package dev.coletz.voidkeyboard

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.R as MaterialR
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var languageValue: TextView
    private lateinit var keyNavigationStatus: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { askOverlayPermission() }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = getString(R.string.settings_title)

        // Edge-to-edge (enforced from Android 15): keep the last item above the navigation bar
        val root = findViewById<View>(R.id.settings_root)
        val rootPaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            view.updatePadding(bottom = rootPaddingBottom + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }

        styleGroupedItems(
            findViewById(R.id.enabled_languages_setting),
            findViewById(R.id.selected_language_setting),
            findViewById(R.id.key_navigation_setting)
        )
        if (!MaterialUi.effectsEnabled) {
            findViewById<TextInputLayout>(R.id.test_input_layout).isHintAnimationEnabled = false
        }

        languageValue = findViewById(R.id.language_value)
        updateLanguageDisplay()

        findViewById<View>(R.id.enabled_languages_setting).setOnClickListener {
            showSystemSettings()
        }

        findViewById<LinearLayout>(R.id.selected_language_setting).setOnClickListener {
            inputMethodManager.showInputMethodPicker()
        }

        keyNavigationStatus = findViewById(R.id.key_navigation_status)
        updateKeyNavigationStatus()

        findViewById<View>(R.id.key_navigation_setting).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        if (savedInstanceState == null) {
            requestPermissions()
        }
    }

    // Notifications first: without them the keyboard can't remind about the overlay permission later
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            askOverlayPermission()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The overlay permission has no runtime dialog, it must be granted from the system settings
    private fun askOverlayPermission() {
        if (OverlayPermissionHelper.hasPermission(this)) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.overlay_permission_title)
            .setMessage(R.string.overlay_permission_dialog_message)
            .setPositiveButton(R.string.open_settings) { _, _ -> OverlayPermissionHelper.openSettings(this) }
            .setNegativeButton(R.string.not_now, null)
            .show()
    }

    // Material 3 Expressive grouped list: large outer corners on the group, small ones between items
    private fun styleGroupedItems(vararg items: View) {
        val outer = MaterialUi.dp(this, GROUP_OUTER_CORNER_DP)
        val inner = MaterialUi.dp(this, GROUP_INNER_CORNER_DP)
        items.forEachIndexed { index, item ->
            val top = if (index == 0) outer else inner
            val bottom = if (index == items.lastIndex) outer else inner
            val radii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
            fun shape(colorAttr: Int) = GradientDrawable().apply {
                setColor(MaterialUi.color(this@SettingsActivity, colorAttr))
                cornerRadii = radii
            }
            item.background = MaterialUi.pressableBackground(this, shape(MaterialR.attr.colorSurfaceContainer)) {
                shape(MaterialR.attr.colorSecondaryContainer)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateLanguageDisplay()
        updateKeyNavigationStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            updateLanguageDisplay()
        }
    }

    fun showSystemSettings() {
        val isInputMethodEnabled = inputMethodManager.enabledInputMethodList.any { it.packageName == packageName }
        if (!isInputMethodEnabled) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            return
        }

        // THIS SHOULD NOT BE REQUIRED
        /*val currentId = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isInputMethodSelected = currentId != null && currentId.startsWith(packageName)
        if (!isInputMethodSelected) {
            inputMethodManager.showInputMethodPicker()
            return
        }*/

        showLanguageEnabler()
    }

    private fun showLanguageEnabler() {
        val imeInfo = inputMethodManager.enabledInputMethodList.find { it.packageName == packageName }
        val inputMethodId = imeInfo?.id ?: "${packageName}/.${ImeService::class.simpleName}"
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
        intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, inputMethodId)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun updateLanguageDisplay() {
        languageValue.text = ImeUtils.getSelectedImeInfo(this)
    }

    private fun updateKeyNavigationStatus() {
        val enabled = getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val service = it.resolveInfo.serviceInfo
                service.packageName == packageName && service.name == KeyNavigationService::class.java.name
            }
        keyNavigationStatus.setText(if (enabled) R.string.key_navigation_enabled else R.string.key_navigation_disabled)
    }

    private companion object {
        const val GROUP_OUTER_CORNER_DP = 24f
        const val GROUP_INNER_CORNER_DP = 4f
    }
}
