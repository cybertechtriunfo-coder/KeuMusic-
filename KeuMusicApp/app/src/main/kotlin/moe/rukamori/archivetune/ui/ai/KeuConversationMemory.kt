/*
 * KeuMusic AI | Optional private conversation memory
 * Conversation history stays in app-private SharedPreferences and is opt-in.
 */
package moe.rukamori.archivetune.ui.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal object KeuConversationMemory {
    private const val PREFS = "keumusic_ai_private_memory"
    private const val KEY_HISTORY = "conversation_history"
    private const val MAX_LINES = 40

    data class Line(val author: String, val message: String)

    fun load(context: Context): List<Line> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val author = item.optString("author").take(40)
                    val message = item.optString("message").take(2_000)
                    if (author.isNotBlank() && message.isNotBlank()) add(Line(author, message))
                }
            }.takeLast(MAX_LINES)
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, lines: List<Line>) {
        val array = JSONArray()
        lines.takeLast(MAX_LINES).forEach { line ->
            array.put(JSONObject().put("author", line.author.take(40)).put("message", line.message.take(2_000)))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, array.toString())
            .apply()
    }

    fun append(context: Context, line: Line) {
        val updated = load(context).toMutableList()
        updated.add(Line(line.author.take(40), line.message.take(2_000)))
        save(context, updated)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}


/**
 * Optional local profile memory. Saving a name is an explicit user action.
 * This profile remains in app-private preferences and is not sent to AI providers.
 */
internal object KeuUserProfileMemory {
    private const val PREFS = "keumusic_ai_private_memory"
    private const val KEY_DISPLAY_NAME = "profile_display_name"

    fun loadName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DISPLAY_NAME, "")
            .orEmpty()
            .trim()
            .take(60)

    fun saveName(context: Context, name: String) {
        val safeName = name.trim().take(60)
        if (safeName.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISPLAY_NAME, safeName)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DISPLAY_NAME)
            .apply()
    }
}
