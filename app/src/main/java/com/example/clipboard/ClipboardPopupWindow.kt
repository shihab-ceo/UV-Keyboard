package com.example.clipboard

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.KeyboardTheme
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ClipboardPopupWindow(
    private val context: Context,
    private val repository: ClipboardRepository = ClipboardRepository(context)
) {

    private val popupScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var dataCollectionJob: Job? = null

    private var popupWindow: PopupWindow? = null
    private var contentView: View? = null

    private var rvClipboardItems: RecyclerView? = null
    private var tvEmptyState: TextView? = null
    private var tvTitle: TextView? = null
    private var topBarLayout: View? = null
    private var rootLayout: View? = null

    private var adapter: ClipboardAdapter? = null
    private var currentTheme: KeyboardTheme? = null
    private var activeInputConnection: InputConnection? = null

    var onItemPastedListener: ((String) -> Unit)? = null
    var onDismissListener: (() -> Unit)? = null

    init {
        initView()
    }

    private fun initView() {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_clipboard_layout, null)
        contentView = view

        rootLayout = view.findViewById(R.id.ll_popup_root)
        topBarLayout = view.findViewById(R.id.ll_popup_top_bar)
        tvTitle = view.findViewById(R.id.tv_clipboard_title)
        val btnClose = view.findViewById<FrameLayout>(R.id.btn_close_popup)
        rvClipboardItems = view.findViewById(R.id.rv_clipboard_items)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)

        // Setup RecyclerView & Adapter
        adapter = ClipboardAdapter(
            onItemClick = { item ->
                // Single Tap on Card: commit text and dismiss
                activeInputConnection?.commitText(item.text, 1)
                onItemPastedListener?.invoke(item.text)
                dismiss()
            },
            onItemDelete = { item, position ->
                // Single Tap on Cross (✕): delete from repository and remove from UI
                repository.deleteClip(item.id)
                adapter?.removeItem(position)
                updateEmptyStateVisibility()
            }
        )

        rvClipboardItems?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ClipboardPopupWindow.adapter
            setHasFixedSize(true)
        }

        // Close Button (✕)
        btnClose.setOnClickListener {
            dismiss()
        }

        // PopupWindow Configuration
        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false // Non-focusable so keyboard input connection is preserved
        ).apply {
            isOutsideTouchable = true
            isClippingEnabled = false
            animationStyle = R.style.Animation_ClipboardPopup
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setOnDismissListener {
                dataCollectionJob?.cancel()
                onDismissListener?.invoke()
            }
        }
        popupWindow = popup
    }

    fun setTheme(theme: KeyboardTheme) {
        this.currentTheme = theme
        adapter?.setTheme(theme)
        applyThemeStyling(theme)
    }

    private fun applyThemeStyling(theme: KeyboardTheme) {
        val root = rootLayout ?: return
        val topBar = topBarLayout ?: return

        // Rounded top background for popup
        val rootBg = GradientDrawable().apply {
            setColor(theme.backgroundColor)
            cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 0f, 0f)
        }
        root.background = rootBg

        val topBarBg = GradientDrawable().apply {
            setColor(theme.keyColor)
            cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 0f, 0f)
        }
        topBar.background = topBarBg

        tvTitle?.setTextColor(theme.accentColor)
        tvEmptyState?.setTextColor(ColorUtils.setAlphaComponent(theme.textColor, 140))
    }

    fun show(anchorView: View, inputConnection: InputConnection? = null) {
        this.activeInputConnection = inputConnection

        if (popupWindow?.isShowing == true) {
            return
        }

        // Set optimal popup height to match keyboard layout height or standard height
        val targetHeight = if (anchorView.height > 0) {
            anchorView.height
        } else {
            (260 * context.resources.displayMetrics.density).toInt()
        }
        popupWindow?.height = targetHeight

        // Apply theme if available
        currentTheme?.let { applyThemeStyling(it) }

        // Observe clipboard data from Room DB
        observeClipboardData()

        try {
            popupWindow?.showAtLocation(anchorView, Gravity.BOTTOM, 0, 0)
        } catch (e: Exception) {
            // Fallback showAsDropDown if window token not ready
            popupWindow?.showAsDropDown(anchorView)
        }
    }

    private fun observeClipboardData() {
        dataCollectionJob?.cancel()
        dataCollectionJob = popupScope.launch {
            repository.getAllClips().collectLatest { clips ->
                adapter?.submitList(clips)
                updateEmptyStateVisibility()
            }
        }
    }

    private fun updateEmptyStateVisibility() {
        val count = adapter?.itemCount ?: 0
        if (count == 0) {
            tvEmptyState?.visibility = View.VISIBLE
            rvClipboardItems?.visibility = View.GONE
        } else {
            tvEmptyState?.visibility = View.GONE
            rvClipboardItems?.visibility = View.VISIBLE
        }
    }

    fun dismiss() {
        dataCollectionJob?.cancel()
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
        }
    }

    fun isShowing(): Boolean = popupWindow?.isShowing == true

    fun onDestroy() {
        dismiss()
        popupScope.cancel()
    }
}
