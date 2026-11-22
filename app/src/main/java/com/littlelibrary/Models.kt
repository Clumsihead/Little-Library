package com.littlelibrary

import android.content.Intent
import android.net.Uri

data class TagCount(val name: String, val count: Int)

data class SavedItem(
    val id: Long,
    val url: String,
    val title: String,
    val source: String,
    val savedAt: Long,
    val tags: List<String>,
)

enum class SaveResult { ADDED, UPDATED }

data class SharedContent(val url: String, val title: String, val source: String)

object TagUtils {
    /** "  #Road   Trips, " -> "road trips" */
    fun normalize(raw: String): String =
        raw.trim()
            .trimStart('#')
            .replace(Regex("[,\\s]+"), " ")
            .trim()
            .lowercase()
            .take(30)

    /**
     * Suggests already-created tags for what is being typed.
     * Empty input -> your most used tags. Otherwise tags containing the text,
     * those that start with it first, then by how often they're used.
     */
    fun suggest(query: String, all: List<TagCount>, exclude: Set<String>, limit: Int = 8): List<TagCount> {
        val q = normalize(query)
        val pool = all.filter { it.name !in exclude }
        if (q.isEmpty()) return pool.take(limit)
        return pool
            .filter { it.name.contains(q) }
            .sortedWith(
                compareByDescending<TagCount> { it.name.startsWith(q) }
                    .thenByDescending { it.count }
                    .thenBy { it.name },
            )
            .take(limit)
    }
}

object ShareParser {
    private val urlRegex = Regex("""https?://\S+""")

    /** Pulls the link (and a title, if the app sent one) out of a Share intent. */
    fun parse(intent: Intent): SharedContent? {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return null
        val match = urlRegex.find(text) ?: return null
        val url = match.value.trimEnd('.', ',', ')', '!')
        val leftover = text.replace(match.value, "").trim()
        val subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()?.trim().orEmpty()
        val title = leftover.ifBlank { subject }.take(120)
        return SharedContent(url, title, describe(url))
    }

    fun describe(url: String): String {
        val uri = Uri.parse(url)
        val host = (uri.host ?: "").removePrefix("www.").removePrefix("m.")
        val path = uri.path.orEmpty()
        return when {
            host.endsWith("instagram.com") -> when {
                path.startsWith("/reel") -> "Instagram Reel"
                path.startsWith("/p/") -> "Instagram Post"
                path.startsWith("/tv/") -> "Instagram Video"
                path.startsWith("/stories") -> "Instagram Story"
                else -> "Instagram"
            }
            host == "youtu.be" || host.endsWith("youtube.com") ->
                if (path.startsWith("/shorts")) "YouTube Short" else "YouTube Video"
            host.isNotEmpty() -> host
            else -> "Link"
        }
    }
}
