package com.notepad

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.notepad.R.drawable.image
import com.notepad.databinding.ActivityMainBinding
import java.io.FileNotFoundException
import android.text.style.MetricAffectingSpan
import android.text.TextPaint
import android.graphics.Typeface
import java.util.Locale

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private var currentNote: String? = null

    private val PREFS_NAME = "NotepadPrefs"
    private val KEY_LAST_NOTE = "last_note"
    private fun colorKeyFor(note: String) = "note_color_$note"

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var currentFontSize = 16f // default, will load from prefs

    data class LanguageItem(val code: String, val label: String)
    val languages = listOf(
        LanguageItem("it", "\uD83C\uDDEE\uD83C\uDDF9 Italiano (Italian)"),
        LanguageItem("en", "\uD83C\uDDEC\uD83C\uDDE7 English (English)"),
        LanguageItem("ar", "\uD83C\uDDF8\uD83C\uDDE6 العربية (Arabic)"),
        LanguageItem("ru", "\uD83C\uDDF7\uD83C\uDDFA Русский (Russian)"),
        LanguageItem("zh", "\uD83C\uDDE8\uD83C\uDDF3 简体中文 (Simplified Chinese)"),
        LanguageItem("ko", "\uD83C\uDDF0\uD83C\uDDF7 한국어 (Korean)"),
        LanguageItem("hi", "\uD83C\uDDEE\uD83C\uDDF3 हिन्दी (Hindi)"),
        LanguageItem("es", "\uD83C\uDDEA\uD83C\uDDF8 Español (Spanish)"),
        LanguageItem("fr", "\uD83C\uDDEB\uD83C\uDDF7 Français (French)"),
        LanguageItem("hy", "\uD83C\uDDE6\uD83C\uDDF2 հայերեն (Armenian)"),
        LanguageItem("de", "\uD83C\uDDE9\uD83C\uDDEA Deutsch (German)")
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // set app as fullscreen to avoid statusbar color mismatch
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Setup Toolbar
        if (currentNote != null) {
            supportActionBar?.title = currentNote
            updateToolbarIcon(currentNote!!)
        } else {
            supportActionBar?.title = getString(R.string.app_name)
        }


        // Setup Drawer
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navigationView.setNavigationItemSelectedListener(this)

        // Load existing note
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastNote = prefs.getString(KEY_LAST_NOTE, null)

        if (lastNote != null && fileList().contains("$lastNote.txt")) {
            currentNote = lastNote
            binding.noteEditText.setText(readNote(lastNote))
            applySavedColor(lastNote)
            showToast(getString(R.string.m9, lastNote))
            updateToolbarIcon(currentNote!!)
            updateEmptyStateVisibility()
        } else {
            currentNote = null
        }

        updateEmptyStateVisibility()
        applyUserTextStyle()
        applyFontToNavigationView()

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentFontSize *= detector.scaleFactor
                currentFontSize = currentFontSize.coerceIn(8f, 60f) // Prevent too small or too big

                binding.noteEditText.textSize = currentFontSize

                // Save updated size to preferences
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                prefs.edit { putFloat("font_size", currentFontSize) }

                return true
            }
        })

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)

    }


    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_save -> {
                if (currentNote == null) {
                    promptNoteName(getString(R.string.cta3)) { name ->
                        currentNote = name
                        updateToolbarTitle()
                        saveLastOpenedNote(name)
                        saveNote(name, binding.noteEditText.text.toString())
                        updateToolbarIcon(currentNote!!)
                        updateDrawerNoteInfo()
                        updateEmptyStateVisibility()
                    }
                } else {
                    saveNote(currentNote!!, binding.noteEditText.text.toString())
                    updateToolbarIcon(currentNote!!)
                    updateDrawerNoteInfo()
                    updateEmptyStateVisibility()
                }

            }

            R.id.nav_new -> {
                promptNoteName(getString(R.string.m7)) { name ->
                    showIconPicker { iconName ->
                        currentNote = name
                        saveLastOpenedNote(name)

                        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        prefs.edit { putString("note_icon_$name", iconName) }

                        binding.noteEditText.setText("")
                        updateToolbarTitle()
                        updateToolbarIcon(name)
                        showToast(getString(R.string.m16, name))
                        updateToolbarIcon(currentNote!!)
                        updateDrawerNoteInfo()
                        updateEmptyStateVisibility()
                    }
                }

            }

            R.id.nav_load -> {
                val files = fileList().filter { it.endsWith(".txt") }
                if (files.isEmpty()) {
                    showToast(getString(R.string.m5))
                    updateEmptyStateVisibility()
                } else {
                    chooseNote(files) { name ->
                        currentNote = name
                        updateToolbarTitle()
                        saveLastOpenedNote(name)
                        binding.noteEditText.setText(readNote(name))
                        applySavedColor(name)
                        showToast(getString(R.string.m6, name))
                        updateToolbarIcon(currentNote!!)
                        updateDrawerNoteInfo()
                    }
                }
                updateEmptyStateVisibility()
            }
            R.id.nav_delete -> {
                if (currentNote == null) {
                    showToast(getString(R.string.e2))
                } else {
                    confirm(getString(R.string.cta9, currentNote)) {
                        val name = currentNote!!
                        deleteFile("$name.txt")
                        showToast(getString(R.string.m4, name))
                        currentNote = null
                        binding.noteEditText.setText("")
                        binding.toolbar.setBackgroundColor(getColor(R.color.noteColorBlue))
                        updateToolbarTitle()
                        updateToolbarIcon(currentNote ?: "")
                        updateDrawerNoteInfo()
                    }
                }
                updateEmptyStateVisibility()
            }
            R.id.nav_color -> {
                showColorPicker()
                updateDrawerNoteInfo()
            }
            R.id.nav_share -> {
                currentNote?.let { name ->
                    val text = binding.noteEditText.text.toString()
                    if (text.isNotEmpty()) {
                        shareNote(name, text)
                    } else {
                        showToast(getString(R.string.e3)) // "Note is empty"
                    }
                } ?: showToast(getString(R.string.e2)) // "No note to share"
                updateDrawerNoteInfo()
            }

            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                updateDrawerNoteInfo()
            }

            R.id.nav_change_icon -> {
                if (currentNote == null) {
                    showToast(getString(R.string.e6)) // "No note selected"
                } else {
                    showIconPicker { iconName ->
                        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        prefs.edit { putString("note_icon_$currentNote", iconName) }
                        updateToolbarIcon(currentNote!!)
                        updateDrawerNoteInfo()
                    }
                }
            }

            R.id.nav_language -> showLanguagePicker()

        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        applyUserTextStyle()
        updateDrawerNoteInfo()
        updateEmptyStateVisibility()
    }


    //function to save the latest opened note to SharedPreferences if the app gets somehow interrupted
    private fun saveLastOpenedNote(name: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit { putString(KEY_LAST_NOTE, name) }
        updateDrawerNoteInfo()
        updateEmptyStateVisibility()
    }

    //function to continuously update the toolbar's title to coincide with the current note's name
    private fun updateToolbarTitle() {
        supportActionBar?.title = currentNote ?: getString(R.string.app_name)
        updateDrawerNoteInfo()
    }


    //function to save the current loaded note to storage
    private fun saveNote(name: String, text: String) {
        try {
            openFileOutput("$name.txt", MODE_PRIVATE).use {
                it.write(text.toByteArray())
            }
            showToast(getString(R.string.m3, name))
            updateToolbarIcon(currentNote!!)

        } catch (e: Exception) {
            showToast(getString(R.string.e1, e.message))
            updateToolbarIcon(currentNote!!)
        }
        updateEmptyStateVisibility()
    }

    //function to load a note from a selection of notes the user has previously saved.
    private fun readNote(name: String): String {
        return try {
            openFileInput("$name.txt").bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            ""
        } catch (e: Exception) {
            showToast(getString(R.string.e4, name, e.message))
            ""
        }
        updateEmptyStateVisibility()
    }

    //function to name a note
    private fun promptNoteName(title: String, onDone: (String) -> Unit) {
        val input = android.widget.EditText(this)
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(getString(R.string.cta15)) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    onDone(name)
                } else {
                    showToast(getString(R.string.e5))
                }
            }
            .setNegativeButton(getString(R.string.cta16), null)
            .show()
            .setIcon(R.drawable.pen)
    }

    //function to choose a note from a selection.
    private fun chooseNote(files: List<String>, onSelected: (String) -> Unit) {
        val names = files.map { it.removeSuffix(".txt") }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.cta13))
            .setItems(names) { _, which ->
                onSelected(names[which])
            }
            .setIcon(R.drawable.open)
            .show()
        updateEmptyStateVisibility()
    }


    //function to pop a toast message in case of error or success with file I/O etc.
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun confirm(message: String, onConfirm: () -> Unit) {
        android.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.cta7)) { _, _ -> onConfirm() }
            .setNegativeButton(getString(R.string.cta8), null)
            .setIcon(R.drawable.check)
            .show()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun showColorPicker() {
        val colors = arrayOf(getString(R.string.c1), getString(R.string.c2), getString(R.string.c3), getString(R.string.c4))
        val colorValues = arrayOf(
            R.color.noteColorRed,
            R.color.noteColorBlue,
            R.color.noteColorGreen,
            R.color.noteColorYellow
        )

        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.cta6)
            .setItems(colors) { dialog, which ->
                val colorRes = colorValues[which]
                val color = getColor(colorRes)
                binding.toolbar.setBackgroundColor(color)
                window.statusBarColor = color
                updateDrawerNoteInfo()
                // Save to preferences
                currentNote?.let { name ->
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    prefs.edit { putInt(colorKeyFor(name), color) }
                }
                dialog.dismiss()
                updateDrawerNoteInfo()
                updateEmptyStateVisibility()
            }
            .setIcon(R.drawable.color)
            .show()
    }

    private fun applySavedColor(name: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val defaultColor = getColor(R.color.noteColorBlue) // fallback color
        val savedColor = prefs.getInt(colorKeyFor(name), defaultColor)
        binding.toolbar.setBackgroundColor(savedColor)
        window.statusBarColor = savedColor
        updateDrawerNoteInfo()
    }

    private fun shareNote(name: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.cta10)))
    }

    private fun applyUserTextStyle() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val size = prefs.getFloat("font_size", 16f)
        val locale = resources.configuration.locales[0]

        val fontRes = when (locale.language) {
            "it" -> R.font.archivo_bold
            "en" -> R.font.archivo_bold
            "ar" -> R.font.noto_sans_arabic_bold
            "ru" -> R.font.archivo_bold
            "zh" -> R.font.noto_sans_sc_bold
            "ko" -> R.font.noto_sans_kr_bold
            "hi" -> R.font.noto_sans_devanagari_bold
            else -> R.font.archivo_narrow_bold
        }

        try {
            val typeface = ResourcesCompat.getFont(this, fontRes)
            if (typeface != null) {
                binding.noteEditText.typeface = typeface
                binding.emptyStateView.typeface = typeface
            }
        } catch (e: Resources.NotFoundException) {
            showToast("Font missing: ${e.message}")
        }

        binding.noteEditText.textSize = size
    }


    private fun showIconPicker(onIconSelected: (String) -> Unit) {
        val iconNames = arrayOf("asterisk", "check", "dollar", "person")
        val iconRes = arrayOf("asterisk", "check", "dollar", "person")

        AlertDialog.Builder(this)
            .setTitle("Choose Icon")
            .setItems(iconNames) { _, which ->
                onIconSelected(iconRes[which])
            }
            .setIcon(R.drawable.image)
            .show()
        updateDrawerNoteInfo()
    }

    @SuppressLint("DiscouragedApi")
    private fun updateToolbarIcon(noteName: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val iconName = prefs.getString("note_icon_$noteName", null)

        if (iconName != null) {
            val resId = resources.getIdentifier(iconName, "drawable", packageName)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setLogo(resId)
            supportActionBar?.setDisplayUseLogoEnabled(true)
        } else {
            supportActionBar?.setDisplayShowHomeEnabled(false)
        }
        updateDrawerNoteInfo()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateDrawerNoteInfo() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val iconName = prefs.getString("note_icon_$currentNote", "ic_note")
        val noteName = currentNote ?: return
        val color = prefs.getInt(colorKeyFor(noteName), getColor(R.color.noteColorBlue))


        val menuItem = binding.navigationView.menu.findItem(R.id.nav_current_note_info)
        menuItem.title = currentNote

        val resId = resources.getIdentifier(iconName, "drawable", packageName)
        if (resId != 0) {
            menuItem.icon = getDrawable(resId)?.apply {
                setTint(color)
            }
        } else {
            // fallback if iconName is not found
            menuItem.icon = getDrawable(image)?.apply {
                setTint(color)
            }
        }

    }

    private fun updateEmptyStateVisibility() {
        val noNoteLoaded = currentNote == null
        binding.noteEditText.visibility = if (noNoteLoaded) View.GONE else View.VISIBLE
        binding.emptyStateView.visibility = if (noNoteLoaded) View.VISIBLE else View.GONE

        if (noNoteLoaded) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val last = prefs.getString(KEY_LAST_NOTE, null)
            val color = last?.let { prefs.getInt(colorKeyFor(it), getColor(R.color.noteColorBlue)) }
                ?: getColor(R.color.noteColorBlue)
            binding.emptyStateView.setTextColor(color)
        }
    }

    private fun applyFontToNavigationView() {
        val navMenu = binding.navigationView.menu
        val locale = resources.configuration.locales[0]

        val fontRes = when (locale.language) {
            "it", "en", "ru" -> R.font.archivo_bold
            "ar" -> R.font.noto_sans_arabic_bold
            "zh" -> R.font.noto_sans_sc_bold
            "ko" -> R.font.noto_sans_kr_bold
            "hi" -> R.font.noto_sans_devanagari_bold
            else -> R.font.archivo_bold
        }
        val isRTL = locale.language == "ar"
        binding.noteEditText.textDirection = if (isRTL) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR


        val typeface = ResourcesCompat.getFont(this, fontRes)
        if (typeface != null) {
            for (i in 0 until navMenu.size()) {
                val item = navMenu.getItem(i)
                val span = SpannableString(item.title)
                span.setSpan(CustomTypefaceSpan(typeface), 0, span.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                item.title = span
            }
        }
    }

    fun showLanguagePicker() {
        val items = languages.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cta14))
            .setItems(items) { _, which ->
                setAppLocale(languages[which].code)
            }
            .show()
    }
    fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        val context = createConfigurationContext(config)
        resources.updateConfiguration(config, context.resources.displayMetrics)

        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putString("app_locale", languageCode)
            .apply()

        recreate()
    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val langCode = sharedPreferences.getString("app_locale", null)

        val context = if (langCode != null) {
            val locale = Locale(langCode)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }

        super.attachBaseContext(context)
    }

}

class CustomTypefaceSpan(private val customTypeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(ds: TextPaint) {
        ds.typeface = customTypeface
    }

    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = customTypeface
    }
}

