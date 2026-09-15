package com.example.ime

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Manages persisted recently used emojis in SharedPreferences.
 */
class RecentEmojiManager(context: Context) {
    private val prefs = context.getSharedPreferences("uv_recent_emojis_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RECENTS = "recent_emojis_list"
        private const val MAX_RECENTS = 40
    }

    fun getRecentEmojis(): List<String> {
        val raw = prefs.getString(KEY_RECENTS, null)
        if (raw.isNullOrBlank()) {
            return EmojiProvider.DEFAULT_RECENTS
        }
        val list = raw.split(",").filter { it.isNotBlank() }
        return if (list.isEmpty()) EmojiProvider.DEFAULT_RECENTS else list
    }

    fun addRecentEmoji(emoji: String) {
        val current = getRecentEmojis().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        if (current.size > MAX_RECENTS) {
            val trimmed = current.subList(0, MAX_RECENTS)
            prefs.edit().putString(KEY_RECENTS, trimmed.joinToString(",")).apply()
        } else {
            prefs.edit().putString(KEY_RECENTS, current.joinToString(",")).apply()
        }
    }
}

/**
 * Gboard-style Full-Keyboard Emoji Keyboard View.
 *
 * Architecture & Features:
 * 1. Full Keyboard MATCH_PARENT Coverage:
 *    - Completely fills the keyboard container width and height with an opaque dark canvas
 *      so no underlying background images, theme layers, or key rows bleed through.
 * 2. Horizontal Swipe Category Navigation:
 *    - Built with ViewPager2 + TabLayout + TabLayoutMediator.
 *    - Users can fluidly swipe left and right across categories (Recents, Smileys, Sad,
 *      Gestures, Animals, Food, Travel, Activities, Objects, Symbols, Flags) with full
 *      tab indicator sync.
 * 3. Fast Dynamic Recents:
 *    - Automatically persists used emojis to SharedPreferences and live-refreshes the Recents tab.
 * 4. Dedicated Utility Bar:
 *    - [ ABC ] returns to normal typing.
 *    - [ Category Label ] displays the active category in real time.
 *    - [ ⌫ Backspace ] supports both single-tap and continuous repeat deletion.
 */
class EmojiKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var inputConnection: InputConnection? = null
    var onEmojiSelected: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onCloseEmoji: (() -> Unit)? = null

    private val recentEmojiManager = RecentEmojiManager(context)
    private val categories = EmojiProvider.getCategories()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var pagerAdapter: EmojiCategoryPagerAdapter
    private lateinit var tvCategoryTitle: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isBackspacePressed = false

    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspacePressed) {
                onBackspace?.invoke()
                mainHandler.postDelayed(this, 50L)
            }
        }
    }

    init {
        orientation = VERTICAL
        // Solid opaque background to prevent any background bleed
        setBackgroundColor(Color.parseColor("#111827"))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        buildTopCategoryTabs()
        buildCategoryViewPager()
        buildBottomActionRow()

        // Connect TabLayout with ViewPager2 for smooth swiping and indicator sync
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            val category = categories[position]
            val tabTextView = TextView(context).apply {
                text = category.icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                val pad = dpToPx(6)
                setPadding(pad, 0, pad, 0)
            }
            tab.customView = tabTextView
        }.attach()

        // Default to Smileys (index 1) if available, otherwise 0
        val defaultIndex = categories.indexOf(EmojiProvider.Category.SMILEYS).let { if (it >= 0) it else 0 }
        viewPager2.setCurrentItem(defaultIndex, false)
        tvCategoryTitle.text = categories[defaultIndex].title
    }

    private fun buildTopCategoryTabs() {
        val themedContext = ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        tabLayout = TabLayout(themedContext).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(44))
            tabMode = TabLayout.MODE_SCROLLABLE
            tabGravity = TabLayout.GRAVITY_FILL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setSelectedTabIndicatorColor(Color.parseColor("#3B82F6"))
            setSelectedTabIndicatorHeight(dpToPx(3))
            tabRippleColor = null
        }

        addView(tabLayout)
    }

    private fun buildCategoryViewPager() {
        viewPager2 = ViewPager2(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1
        }

        pagerAdapter = EmojiCategoryPagerAdapter(
            categories = categories,
            recentEmojiManager = recentEmojiManager,
            onEmojiClicked = { emoji, view ->
                // Micro-bounce visual touch feedback
                view.animate()
                    .scaleX(1.25f)
                    .scaleY(1.25f)
                    .setDuration(50)
                    .withEndAction {
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start()
                    }.start()

                // a) Safely commit text to input connection
                try {
                    inputConnection?.commitText(emoji, 1)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onEmojiSelected?.invoke(emoji)

                // b) View persistence: Never alter, reset, or set visibility = GONE during emoji taps
                // c) Save to Recents via SharedPreferences without disruptive layout invalidation
                recentEmojiManager.addRecentEmoji(emoji)
                if (viewPager2.currentItem != 0) {
                    post {
                        pagerAdapter.notifyRecentsUpdated()
                    }
                }
            }
        )

        viewPager2.adapter = pagerAdapter

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position in categories.indices) {
                    tvCategoryTitle.text = categories[position].title
                }
            }
        })

        addView(viewPager2)
    }

    private fun buildBottomActionRow() {
        val bottomRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(44))
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
        }

        // ABC / Return to typing Button
        val btnAbc = TextView(context).apply {
            text = "ABC"
            textSize = 14f
            setTextColor(Color.parseColor("#E2E8F0"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dpToPx(62), dpToPx(34))
            background = createRoundedDrawable(Color.parseColor("#1E293B"), dpToPx(6).toFloat())
            setOnClickListener {
                onCloseEmoji?.invoke()
            }
        }
        bottomRow.addView(btnAbc)

        // Center Category Title
        tvCategoryTitle = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            gravity = Gravity.CENTER
            text = "Smileys & Emotion"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
        }
        bottomRow.addView(tvCategoryTitle)

        // Dedicated Backspace Key (⌫)
        val btnBackspace = TextView(context).apply {
            text = "⌫"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dpToPx(56), dpToPx(34))
            background = createRoundedDrawable(Color.parseColor("#374151"), dpToPx(6).toFloat())

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isBackspacePressed = true
                        onBackspace?.invoke()
                        mainHandler.postDelayed(backspaceRepeatRunnable, 350L)
                        background = createRoundedDrawable(Color.parseColor("#4B5563"), dpToPx(6).toFloat())
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isBackspacePressed = false
                        mainHandler.removeCallbacks(backspaceRepeatRunnable)
                        background = createRoundedDrawable(Color.parseColor("#374151"), dpToPx(6).toFloat())
                        true
                    }
                    else -> false
                }
            }
        }
        bottomRow.addView(btnBackspace)

        addView(bottomRow)
    }

    private fun createRoundedDrawable(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onCloseEmoji?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * ViewPager2 Adapter hosting an 8-column GridLayoutManager for each emoji category page.
     */
    private class EmojiCategoryPagerAdapter(
        private val categories: List<EmojiProvider.Category>,
        private val recentEmojiManager: RecentEmojiManager,
        private val onEmojiClicked: (String, View) -> Unit
    ) : RecyclerView.Adapter<EmojiCategoryPagerAdapter.PageViewHolder>() {

        private val gridAdapters = mutableMapOf<Int, EmojiGridAdapter>()

        fun notifyRecentsUpdated() {
            gridAdapters[0]?.updateEmojis(recentEmojiManager.getRecentEmojis())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val recyclerView = RecyclerView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutManager = GridLayoutManager(parent.context, 8)
                setHasFixedSize(true)
                overScrollMode = View.OVER_SCROLL_NEVER
                clipToPadding = false
                val pad = (parent.context.resources.displayMetrics.density * 2).toInt()
                setPadding(pad, pad, pad, pad)
            }
            return PageViewHolder(recyclerView)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val category = categories[position]
            val adapter = EmojiGridAdapter { emoji, view ->
                onEmojiClicked(emoji, view)
            }
            gridAdapters[position] = adapter

            val emojis = if (category == EmojiProvider.Category.RECENTS) {
                recentEmojiManager.getRecentEmojis()
            } else {
                EmojiProvider.getEmojis(category)
            }
            adapter.updateEmojis(emojis)
            holder.recyclerView.adapter = adapter
        }

        override fun onViewRecycled(holder: PageViewHolder) {
            super.onViewRecycled(holder)
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                gridAdapters.remove(pos)
            }
        }

        override fun getItemCount(): Int = categories.size

        class PageViewHolder(val recyclerView: RecyclerView) : RecyclerView.ViewHolder(recyclerView)
    }

    /**
     * Adapter for the 8-column emoji grid.
     */
    private class EmojiGridAdapter(
        private val onEmojiClicked: (String, View) -> Unit
    ) : RecyclerView.Adapter<EmojiGridAdapter.EmojiViewHolder>() {

        private val items = ArrayList<String>()

        fun updateEmojis(newItems: List<String>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
            val textView = TextView(parent.context).apply {
                textSize = 24f
                gravity = Gravity.CENTER
                val cellHeight = (parent.context.resources.displayMetrics.density * 46).toInt()
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeight)

                val outValue = TypedValue()
                parent.context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }
            return EmojiViewHolder(textView)
        }

        override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
            val emoji = items[position]
            holder.textView.text = emoji
            holder.textView.setOnClickListener {
                onEmojiClicked(emoji, holder.textView)
            }
        }

        override fun getItemCount(): Int = items.size

        class EmojiViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    }
}
