package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.clipboard.AppDatabase
import com.example.clipboard.ClipboardItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ClipboardManagerHelper(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.clipboardDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    init {
        // Run cleanup on initialization
        cleanupOldClips()
    }

    fun startListening() {
        if (clipListener != null) return
        clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            checkAndSavePrimaryClip()
        }
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
        // Also check right away
        checkAndSavePrimaryClip()
    }

    fun stopListening() {
        clipListener?.let {
            clipboardManager?.removePrimaryClipChangedListener(it)
            clipListener = null
        }
    }

    fun checkAndSavePrimaryClip() {
        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                saveClip(text)
            }
        }
    }

    fun saveClip(text: String) {
        scope.launch {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@launch

            val existing = dao.findByText(trimmed)
            if (existing != null) {
                // If it exists, update timestamp to keep it recent
                dao.update(existing.copy(timestamp = System.currentTimeMillis()))
            } else {
                dao.insert(
                    ClipboardItem(
                        text = trimmed,
                        timestamp = System.currentTimeMillis(),
                        isPinned = false
                    )
                )
            }
            cleanupOldClipsInternal()
        }
    }

    fun togglePin(item: ClipboardItem) {
        scope.launch {
            dao.update(item.copy(isPinned = !item.isPinned))
        }
    }

    fun deleteClip(id: Long) {
        scope.launch {
            dao.deleteById(id)
        }
    }

    fun clearUnpinnedClips() {
        scope.launch {
            dao.clearUnpinned()
        }
    }

    fun cleanupOldClips() {
        scope.launch {
            cleanupOldClipsInternal()
        }
    }

    private suspend fun cleanupOldClipsInternal() {
        // 3 days = 72 hours = 72 * 60 * 60 * 1000 milliseconds
        val threeDaysInMillis = 72L * 60 * 60 * 1000L
        val cutoff = System.currentTimeMillis() - threeDaysInMillis
        dao.deleteOldUnpinned(cutoff)
    }

    fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboardManager?.setPrimaryClip(clip)
    }

    fun getMostRecentClipText(): String? {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                return text
            }
        }
        return null
    }

    suspend fun getLatestClipFromDb(): String? {
        return dao.getMostRecentClip()?.text ?: getMostRecentClipText()
    }

    fun getAllClips(): Flow<List<ClipboardItem>> = dao.getAllClips()

    fun getRecentClips(): Flow<List<ClipboardItem>> = dao.getRecentClips()

    companion object {
        const val EXPIRY_HOURS = 72
        const val EXPIRY_DAYS = 3
    }
}
