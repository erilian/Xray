package io.github.saeeddev94.xray.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blacksquircle.ui.editorkit.plugin.autoindent.autoIndentation
import com.blacksquircle.ui.editorkit.plugin.base.PluginSupplier
import com.blacksquircle.ui.editorkit.plugin.delimiters.highlightDelimiters
import com.blacksquircle.ui.editorkit.plugin.linenumbers.lineNumbers
import com.blacksquircle.ui.editorkit.widget.TextProcessor
import com.blacksquircle.ui.language.json.JsonLanguage
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.tabs.TabLayout
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.adapter.ConfigAdapter
import io.github.saeeddev94.xray.databinding.ActivityRawConfigBinding
import io.github.saeeddev94.xray.helper.FileHelper
import io.github.saeeddev94.xray.helper.RawConfigHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class RawConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRawConfigBinding
    private lateinit var settings: Settings
    private lateinit var adapter: ConfigAdapter
    private val configEditor = mutableMapOf<String, TextProcessor>()
    private val indentSpaces = 4
    private val rawConfigFile by lazy { File(filesDir, "raw_config.json") }
    private var isEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.rawConfig)
        settings = Settings(applicationContext)
        binding = ActivityRawConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isEnabled = settings.rawConfigEnabled
        binding.enableSwitch.isChecked = isEnabled
        updateEditorVisibility()

        binding.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.rawConfigEnabled = isChecked
            isEnabled = isChecked
            updateEditorVisibility()
        }

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val context = this@RawConfigActivity
                val tabs = listOf("raw")
                adapter = ConfigAdapter(context, tabs) { tab, view -> setup(tab, view) }
                binding.viewPager.adapter = adapter
                binding.tabLayout.setupWithViewPager(binding.viewPager)
                binding.tabLayout.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_raw_config, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.saveRawConfig -> saveRawConfig()
            else -> finish()
        }
        return true
    }

    private fun setup(tab: String, view: View) {
        val config = getRawConfig()

        val modeRadioGroup = view.findViewById<RadioGroup>(R.id.modeRadioGroup)
        modeRadioGroup.visibility = View.GONE

        val editor = view.findViewById<TextProcessor>(R.id.config)
        val pluginSupplier = PluginSupplier.create {
            lineNumbers {
                lineNumbers = true
                highlightCurrentLine = true
            }
            highlightDelimiters()
            autoIndentation {
                autoIndentLines = true
                autoCloseBrackets = true
                autoCloseQuotes = true
            }
        }
        editor.language = JsonLanguage()
        editor.setTextContent(config)
        editor.plugins(pluginSupplier)
        configEditor.put(tab, editor)
    }

    private fun getRawConfig(): String {
        val editor = configEditor["raw"]
        return if (editor == null) {
            if (rawConfigFile.exists()) {
                rawConfigFile.readText()
            } else {
                "{}"
            }
        } else {
            editor.text.toString()
        }
    }

    private fun formatConfig(json: String): String {
        return try {
            JSONObject(json).toString(indentSpaces)
        } catch (e: Exception) {
            json
        }
    }

    private fun saveRawConfig() {
        val editor = configEditor["raw"]
        val config = editor?.text?.toString() ?: "{}"

        runCatching {
            val formatted = formatConfig(config)
            FileHelper.createOrUpdate(rawConfigFile, formatted)
        }.onSuccess {
            Toast.makeText(
                this, getString(R.string.saveRawConfig), Toast.LENGTH_SHORT
            ).show()
        }.onFailure {
            Toast.makeText(
                this, "Invalid config: ${it.message}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateEditorVisibility() {
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        if (isEnabled) {
            viewPager.visibility = View.VISIBLE
            tabLayout.visibility = View.VISIBLE
        } else {
            viewPager.visibility = View.GONE
            tabLayout.visibility = View.GONE
        }
    }
}
