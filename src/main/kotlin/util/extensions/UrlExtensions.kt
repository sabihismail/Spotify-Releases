package util.extensions

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.AbstractMap.SimpleImmutableEntry


object UrlExtensions {
    fun URI?.splitQuery(): Map<String, List<String>> {
        if (this == null || this.query.isNullOrEmpty()) return mapOf()

        return this.query.split("&")
            .map { splitQueryParameter(it) }
            .groupBy { it.key }
            .map { it.key to it.value.map { inner -> inner.value } }
            .toMap()
    }

    private fun splitQueryParameter(it: String): SimpleImmutableEntry<String, String> {
        val idx = it.indexOf("=")
        val key = if (idx > 0) it.substring(0, idx) else it
        val value = if (idx > 0 && it.length > idx + 1) it.substring(idx + 1) else null

        return SimpleImmutableEntry(
            URLDecoder.decode(key, StandardCharsets.UTF_8),
            URLDecoder.decode(value, StandardCharsets.UTF_8)
        )
    }
}