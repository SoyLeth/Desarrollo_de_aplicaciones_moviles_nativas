package com.example.tarea2

import android.os.Bundle
import android.widget.Toast
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import kotlin.math.roundToInt
import com.google.android.material.materialswitch.MaterialSwitch


class MainActivity : AppCompatActivity() {

    // Helper: crea un Drawable a partir de un emoji
    private fun emojiDrawable(emoji: String, sizeDp: Float = 18f): BitmapDrawable {
        val d = resources.displayMetrics.density
        val sizePx = (24f * d).roundToInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizeDp * d
            textAlign = Paint.Align.CENTER
            color = Color.WHITE // o usa Color.BLACK si prefieres
        }

        val x = sizePx / 2f
        val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(emoji, x, y, paint)

        return BitmapDrawable(resources, bmp)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeApplier.apply(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        val btnHq = findViewById<View>(R.id.cardHq)
        val btnStudio = findViewById<View>(R.id.cardStudio)
        val btnPractice = findViewById<View>(R.id.cardPractice)
        val btnNation = findViewById<View>(R.id.cardNation)


        fun View.pop() {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            animate().scaleX(1.12f).scaleY(1.12f).setDuration(90).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }.start()
        }

        btnHq.setOnClickListener {
            it.pop()
            HQBottomSheet().show(supportFragmentManager, "hq")
        }
        btnStudio.setOnClickListener {
            it.pop()
            StudioBottomSheet().show(supportFragmentManager, "studio")
        }
        btnPractice.setOnClickListener {
            it.pop()
            PracticeBottomSheet().show(supportFragmentManager, "practice")
        }
        btnNation.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            startActivity(Intent(this, GroupsActivity::class.java))
        }

        val themeSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchTheme)

// Estado inicial
        val dark = ThemePrefs.isDark(this)
        themeSwitch.isChecked = dark
        themeSwitch.text = if (dark) "🌙" else "☀️"   // emoji informativo

        themeSwitch.setOnCheckedChangeListener { _, checked ->
            ThemePrefs.setDark(this, checked)
            themeSwitch.text = if (checked) "🌙" else "☀️"
            recreate()
        }


    }
}
