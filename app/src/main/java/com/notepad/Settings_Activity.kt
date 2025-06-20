package com.notepad

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.notepad.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val PREFS_NAME = "NotepadPrefs"
    private val KEY_FONT = "font_family"
    private val KEY_FONT_SIZE = "font_size"

    private val fonts = arrayOf("sans-serif", "serif", "monospace")
    private val fontNames = arrayOf("Sans Serif", "Serif", "Monospace")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.cta11)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentFont = prefs.getString(KEY_FONT, "sans-serif")
        val currentSize = prefs.getFloat(KEY_FONT_SIZE, 16f)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontNames)
        binding.fontSpinner.adapter = adapter
        binding.fontSpinner.setSelection(fonts.indexOf(currentFont))

        binding.sizeSeekbar.progress = currentSize.toInt()
        binding.sizeValue.text = currentSize.toInt().toString()

        binding.sizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.sizeValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.saveButton.setOnClickListener {
            val selectedFont = fonts[binding.fontSpinner.selectedItemPosition]
            val selectedSize = binding.sizeSeekbar.progress.toFloat()

            prefs.edit()
                .putString(KEY_FONT, selectedFont)
                .putFloat(KEY_FONT_SIZE, selectedSize)
                .apply()

            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
