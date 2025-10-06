package com.example.tarea2

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2

class GroupsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: GroupPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeApplier.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)

        // Logo superior: volver al MainActivity
        findViewById<ImageView>(R.id.jypTopLogo).setOnClickListener {
            finish()
        }

        viewPager = findViewById(R.id.viewPagerGroups)

        // Datos del carrusel (logos, fotos y colores oficiales)
        val data = listOf(
            GroupInfo(
                id = "twice",
                name = "TWICE",
                slogan = "One in a Million",
                logoResId = R.drawable.twice_logo,
                photoResId = R.drawable.twice_photo,
                backgroundColor = Color.parseColor("#fc5d9d")
            ),
            GroupInfo(
                id = "straykids",
                name = "Stray Kids",
                slogan = "You make Stray Kids stay",
                logoResId = R.drawable.straykids_logo,
                photoResId = R.drawable.straykids_photo,
                backgroundColor = Color.parseColor("#ab0033")
            ),
            GroupInfo(
                id = "itzy",
                name = "ITZY",
                slogan = "ALL IN US",
                logoResId = R.drawable.itzy_logo,
                photoResId = R.drawable.itzy_photo,
                backgroundColor = Color.parseColor("#FFB3DE") // magenta pastel
            ),
            GroupInfo(
                id = "nmixx",
                name = "NMIXX",
                slogan = "NMIXX, Change Up!",
                logoResId = R.drawable.nmixx_logo,
                photoResId = R.drawable.nmixx_photo,
                backgroundColor = Color.parseColor("#555C66")
            ),
            GroupInfo(
                id = "xdh",
                name = "Xdinary Heroes",
                slogan = "We Are All Heroes",
                logoResId = R.drawable.xdh_logo,
                photoResId = R.drawable.xdh_photo,
                backgroundColor = Color.parseColor("#A7B8C8")
            )
        )

        adapter = GroupPagerAdapter(
            items = data,
            onColorReady = { position, color ->
                if (position == viewPager.currentItem) setRootBackground(color)
            },
            onBackImageTap = { group ->
                val i = Intent(this, GroupDetailActivity::class.java)
                i.putExtra("group_id", group.id)
                i.putExtra("group_name", group.name)
                startActivity(i)
            }
        )
        viewPager.adapter = adapter

        // ViewPager2 horizontal + efecto margen/escala
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 1
        viewPager.clipToPadding = false
        viewPager.clipChildren = false
        viewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        viewPager.setPageTransformer(CompositeTransformerWithMargin(24))

        // ---- Fondo dinámico con fade continuo y al "encajar" página ----
        val colors = data.map { it.backgroundColor }
        var currentBgColor = colors.first()
        setRootBackground(currentBgColor)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val c1 = colors[position]
                val c2 = colors[(position + 1).coerceAtMost(colors.lastIndex)]
                val blended = ColorUtils.blendARGB(c1, c2, positionOffset)
                setRootBackground(blended)
            }

            override fun onPageSelected(position: Int) {
                val target = colors[position]
                if (currentBgColor != target) {
                    animateBackgroundTo(target, 220L)
                    currentBgColor = target
                }
            }
        })
    }

    private fun setRootBackground(color: Int) {
        val root = findViewById<View>(R.id.rootContainer)
        root.setBackgroundColor(color)
        root.setTag(R.id.tag_current_bg_color, color)
    }

    private fun animateBackgroundTo(targetColor: Int, durationMs: Long) {
        val root = findViewById<View>(R.id.rootContainer)
        val startColor = (root.getTag(R.id.tag_current_bg_color) as? Int) ?: targetColor

        if (startColor == targetColor) {
            root.setBackgroundColor(targetColor)
            root.setTag(R.id.tag_current_bg_color, targetColor)
            return
        }

        ValueAnimator().apply {
            setIntValues(startColor, targetColor)
            setEvaluator(ArgbEvaluator())
            duration = durationMs
            addUpdateListener {
                val c = it.animatedValue as Int
                root.setBackgroundColor(c)
                root.setTag(R.id.tag_current_bg_color, c)
            }
            start()
        }
    }

    private class CompositeTransformerWithMargin(private val marginDp: Int) : ViewPager2.PageTransformer {
        private val marginPx by lazy { (marginDp * Resources.getSystem().displayMetrics.density).toInt() }
        private val marginTransformer = MarginPageTransformer(marginPx)
        override fun transformPage(page: View, position: Float) {
            marginTransformer.transformPage(page, position)
            val scale = 0.95f + (1 - kotlin.math.abs(position)) * 0.05f
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = 0.9f + (1 - kotlin.math.abs(position)) * 0.1f
        }
    }
}
