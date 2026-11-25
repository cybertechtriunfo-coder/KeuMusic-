/*
 * KeuMusic AI | Explicit long-term memory
 * Inspired by OpenJarvis' searchable-memory pattern, implemented natively for Android.
 * Facts are saved only while the user's local memory switch is enabled.
 */
package moe.rukamori.archivetune.ui.ai

import android.content.Context
import org.json.JSONArray

internal object KeuLongTermMemory {
    private const val PREFS = "keumusic_ai_private_memory"
    private const val KEY_FACTS = "long_term_facts"
    private const val MAX_FACTS = 40
    private const val MAX_FACT_LENGTH = 240

    @Synchronized
    fun remember(context: Context, rawFact: String): Boolean {
        val fact = rawFact.replace(Regex("\\s+"), " ").trim().take(MAX_FACT_LENGTH)
        if (fact.length < 3) return false
        val facts = load(context).toMutableList()
        if (facts.any { it.equals(fact, ignoreCase = true) }) return true
        facts.add(fact)
        save(context, facts.takeLast(MAX_FACTS))
        return true
    }

    @Synchronized
    fun find(context: Context, topic: String = ""): List<String> {
        val facts = load(context)
        val terms = topic.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 }
        if (terms.isEmpty()) return facts.takeLast(10).reversed()
        return facts.asReversed()
            .filter { fact -> terms.any { term -> fact.contains(term, ignoreCase = true) } }
            .take(10)
    }

    @Synchronized
    fun forget(context: Context, topic: String): Int {
        val query = topic.replace(Regex("\\s+"), " ").trim()
        if (query.isBlank()) return 0
        val facts = load(context)
        val remaining = facts.filterNot { it.contains(query, ignoreCase = true) }
        save(context, remaining)
        return facts.size - remaining.size
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FACTS)
            .apply()
    }

    private fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FACTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val fact = array.optString(index).trim().take(MAX_FACT_LENGTH)
                    if (fact.isNotBlank()) add(fact)
                }
            }.takeLast(MAX_FACTS)
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, facts: List<String>) {
        val array = JSONArray()
        facts.takeLast(MAX_FACTS).forEach(array::put)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FACTS, array.toString())
            .apply()
    }
}
