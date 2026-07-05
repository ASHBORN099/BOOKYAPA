package com.bookyapa.app.domain.epub

import android.util.Xml
import com.bookyapa.app.data.model.EpubBook
import com.bookyapa.app.data.model.EpubChapter
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubParser @Inject constructor() {

    fun parse(inputStream: InputStream): Result<EpubBook> = runCatching {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }

        val opfPath = findOpfPath(entries)
        val opfDir = opfPath.substringBeforeLast("/", "").let { if (it.isNotEmpty()) "$it/" else "" }
        val opfXml = entries[opfPath] ?: error("OPF file not found: $opfPath")

        val (title, author, manifest, spine) = parseOpf(opfXml)
        val coverHref = findCoverHref(manifest, entries, opfDir)

        val coverBytes = coverHref?.let { href ->
            entries[opfDir + href] ?: entries[href]
        }

        val toc = parseToc(entries, opfDir)
        val chapters = mutableListOf<EpubChapter>()

        for ((index, idref) in spine.withIndex()) {
            val href = manifest[idref] ?: continue
            val content = entries[opfDir + href] ?: entries[href] ?: continue
            val html = String(content, Charsets.UTF_8)
            val bodyHtml = extractBodyHtml(html)
            val chapterTitle = toc.getOrElse(index) { "Chapter ${index + 1}" }

            chapters.add(EpubChapter(title = chapterTitle, contentHtml = bodyHtml))
        }

        EpubBook(
            title = title,
            author = author,
            coverBytes = coverBytes,
            chapters = chapters,
        )
    }

    private fun findOpfPath(entries: Map<String, ByteArray>): String {
        val containerXml = entries["META-INF/container.xml"]
            ?: error("META-INF/container.xml not found")

        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(containerXml), "UTF-8")

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                val path = parser.getAttributeValue(null, "full-path") ?: continue
                if (path.isNotBlank()) return path
            }
        }
        error("No rootfile found in container.xml")
    }

    private data class OpfData(
        val title: String,
        val author: String?,
        val manifest: Map<String, String>,
        val spine: List<String>,
    )

    private fun parseOpf(opfXml: ByteArray): OpfData {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(opfXml), "UTF-8")

        var title = ""
        var author: String? = null
        val manifest = mutableMapOf<String, String>()
        val spine = mutableListOf<String>()
        var inMetadata = false

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "metadata" -> inMetadata = true
                        "manifest" -> inMetadata = false
                        "title" -> {
                            if (inMetadata) title = parser.nextText().trim()
                        }
                        "creator" -> {
                            if (inMetadata && author == null) author = parser.nextText().trim()
                        }
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            if (id.isNotBlank() && href.isNotBlank()) manifest[id] = href
                        }
                        "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref") ?: ""
                            if (idref.isNotBlank()) spine.add(idref)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "metadata") inMetadata = false
                }
            }
        }

        return OpfData(title, author, manifest, spine)
    }

    private fun findCoverHref(
        manifest: Map<String, String>,
        entries: Map<String, ByteArray>,
        opfDir: String,
    ): String? {
        for ((id, href) in manifest) {
            if (id.contains("cover", ignoreCase = true) &&
                !href.endsWith(".xhtml", ignoreCase = true) &&
                !href.endsWith(".html", ignoreCase = true)
            ) {
                return href
            }
        }
        for ((id, href) in manifest) {
            if (href.contains("cover", ignoreCase = true)) {
                return href
            }
        }
        return null
    }

    private fun parseToc(entries: Map<String, ByteArray>, opfDir: String): List<String> {
        val ncxEntry = entries.entries.firstOrNull { (name, _) ->
            name.endsWith(".ncx", ignoreCase = true)
        } ?: return parseNavXhtmlToc(entries, opfDir)

        val ncxXml = ncxEntry.value
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(ncxXml), "UTF-8")

        val toc = mutableListOf<String>()
        var inNavLabel = false

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "navLabel") {
                inNavLabel = true
            }
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "text" && inNavLabel) {
                toc.add(parser.nextText().trim())
                inNavLabel = false
            }
            if (parser.eventType == XmlPullParser.END_TAG && parser.name == "navLabel") {
                inNavLabel = false
            }
        }

        return toc
    }

    private fun parseNavXhtmlToc(entries: Map<String, ByteArray>, opfDir: String): List<String> {
        val navEntry = entries.entries.firstOrNull { (name, _) ->
            name.contains("nav", ignoreCase = true) && name.endsWith(".xhtml", ignoreCase = true)
        } ?: return emptyList()

        val html = String(navEntry.value, Charsets.UTF_8)
        val toc = mutableListOf<String>()
        val aRegex = Regex("<a[^>]*>(.*?)</a>", RegexOption.IGNORE_CASE)

        aRegex.findAll(html).forEach { match ->
            toc.add(match.groupValues[1].trim())
        }

        return toc
    }

    private fun extractBodyHtml(html: String): String {
        val bodyMatch = Regex(
            "<body[^>]*>(.*?)</body>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)

        return bodyMatch?.groupValues?.get(1)?.trim() ?: html
    }
}
