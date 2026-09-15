package com.example.clipboard

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.KeyboardTheme
import com.example.R

class ClipboardAdapter(
    private val onItemClick: (ClipboardItem) -> Unit,
    private val onItemDelete: (ClipboardItem, Int) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.ClipboardViewHolder>() {

    private val items = mutableListOf<ClipboardItem>()
    private var theme: KeyboardTheme? = null

    fun submitList(newItems: List<ClipboardItem>) {
        items.clear()
        items.addAll(newItems)
        try {
            notifyDataSetChanged()
        } catch (_: Exception) {}
    }

    fun removeItem(position: Int): ClipboardItem? {
        if (position in 0 until items.size) {
            val removed = items.removeAt(position)
            try {
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, items.size - position)
            } catch (_: Exception) {}
            return removed
        }
        return null
    }

    fun getItems(): List<ClipboardItem> = items.toList()

    fun setTheme(keyboardTheme: KeyboardTheme?) {
        this.theme = keyboardTheme
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clipboard_popup, parent, false)
        return ClipboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipboardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ClipboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContainer: View = itemView.findViewById(R.id.ll_clip_item_card)
        private val tvClipText: TextView = itemView.findViewById(R.id.tv_clip_text)
        private val btnDelete: FrameLayout = itemView.findViewById(R.id.btn_delete_clip)
        private val tvDeleteIcon: TextView? = btnDelete.getChildAt(0) as? TextView

        fun bind(item: ClipboardItem) {
            tvClipText.text = item.text

            val currentTheme = theme
            if (currentTheme != null) {
                // Apply theme styling
                val cardBg = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(currentTheme.keyColor)
                    val strokeColor = if (item.isPinned) {
                        currentTheme.accentColor
                    } else {
                        ColorUtils.setAlphaComponent(currentTheme.textColor, 35)
                    }
                    setStroke(2, strokeColor)
                }
                cardContainer.background = cardBg
                tvClipText.setTextColor(currentTheme.textColor)
                tvDeleteIcon?.setTextColor(ColorUtils.setAlphaComponent(currentTheme.textColor, 160))
            } else {
                // Fallback elegant dark styling
                val fallbackBg = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(0xFF1E293B.toInt())
                    setStroke(1, 0xFF334155.toInt())
                }
                cardContainer.background = fallbackBg
            }

            // Single Tap on Text Card -> Paste text into input connection
            cardContainer.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    onItemClick(items[pos])
                }
            }

            // Single Tap on Cross (✕) Icon -> Delete item
            btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    onItemDelete(items[pos], pos)
                }
            }
        }
    }
}
