package dev.coletz.voidkeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var languageValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = getString(R.string.settings_title)

        languageValue = findViewById(R.id.language_value)
        updateLanguageDisplay()

        findViewById<View>(R.id.enabled_languages_setting).setOnClickListener {
            showSystemSettings()
        }

        findViewById<LinearLayout>(R.id.selected_language_setting).setOnClickListener {
            inputMethodManager.showInputMethodPicker()
        }
    }

    override fun onResume() {
        super.onResume()
        updateLanguageDisplay()
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
}
