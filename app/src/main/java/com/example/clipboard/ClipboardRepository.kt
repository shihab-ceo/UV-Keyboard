package com.example.clipboard

import android.content.Context
import com.example.ClipboardManagerHelper
import kotlinx.coroutines.flow.Flow

class ClipboardRepository(context: Context) {
    private val helper = ClipboardManagerHelper(context)

    fun getAllClips(): Flow<List<ClipboardItem>> = helper.getAllClips()

    fun getRecentClips(): Flow<List<ClipboardItem>> = helper.getRecentClips()

    fun saveClip(text: String) {
        helper.saveClip(text)
    }

    fun deleteClip(id: Long) {
        helper.deleteClip(id)
    }

    fun togglePin(item: ClipboardItem) {
        helper.togglePin(item)
    }

    fun clearUnpinned() {
        helper.clearUnpinnedClips()
    }

    fun copyToClipboard(text: String) {
        helper.copyToClipboard(text)
    }

    fun getLatestClipText(): String? {
        return helper.getMostRecentClipText()
    }
}
