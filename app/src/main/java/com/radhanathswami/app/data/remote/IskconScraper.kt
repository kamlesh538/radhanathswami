package com.radhanathswami.app.data.remote

import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.data.model.BrowseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IskconScraper @Inject constructor(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val BASE_URL = "https://audio.iskcondesiretree.com"
        private const val RADHANATH_LECTURES_PATH =
            "/02_-_ISKCON_Swamis/ISKCON_Swamis_-_R_to_Y/His_Holiness_Radhanath_Swami/Lectures"

        val ROOT_CATEGORIES = listOf(
            "Year wise" to "00_-_Year_wise",
            "Theme wise" to "01_-_Theme_wise",
            "Yatra" to "02_-_Yatra",
            "Short Clips" to "Short_Clips",
            "Amrit Droplets" to "05_-_Amrit_Droplets",
            "The Journey Home" to "The_Journey_Home-Audio_Book",
            "Ramayana" to "Ramayana",
            "Hindi Translation" to "Hindi_Translation"
        )
    }

    private fun buildUrl(path: String): String {
        val encoded = path.split("/").joinToString("%2F") { URLEncoder.encode(it, "UTF-8") }
        return "$BASE_URL/index.php?q=f&f=$encoded"
    }

    private fun buildAudioUrl(path: String, filename: String): String {
        return "$BASE_URL$path/$filename"
    }

    private suspend fun fetchDocument(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        val html = response.body?.string() ?: ""
        Jsoup.parse(html, url)
    }

    suspend fun browseDirectory(directoryPath: String): List<BrowseItem> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(directoryPath)
            val doc = fetchDocument(url)
            val items = mutableListOf<BrowseItem>()
            val anchors = doc.select("a[href]")
            for (anchor in anchors) {
                val href = anchor.attr("href")
                val text = anchor.text().trim()

                if (text.isEmpty()) continue

                when {
                    // Folder link: index.php?q=f&f=...
                    href.contains("index.php?q=f&f=") -> {
                        val encodedPath = href.substringAfter("f=")
                        val decodedPath = URLDecoder.decode(encodedPath, "UTF-8")

                        // Only include direct children of the current directory
                        if (!decodedPath.startsWith("$directoryPath/")) continue
                        val relativePath = decodedPath.removePrefix("$directoryPath/")
                        if (relativePath.isBlank() || relativePath.contains("/")) continue

                        val countText = anchor.parent()?.text() ?: ""
                        val count = extractItemCount(countText)
                        val folderName = cleanFolderName(text)

                        items.add(BrowseItem.Folder(folderName, decodedPath, count))
                    }

                    // Direct MP3 file download
                    href.endsWith(".mp3", ignoreCase = true) -> {
                        val audioUrl = if (href.startsWith("http")) href
                        else "$BASE_URL/$href".replace("//", "/").replace("$BASE_URL/", "$BASE_URL/")

                        val cleanTitle = cleanAudioTitle(text.ifEmpty { href.substringAfterLast("/") })
                        val id = href.substringAfterLast("/").removeSuffix(".mp3")
                        items.add(
                            BrowseItem.Audio(
                                AudioItem(
                                    id = id,
                                    title = cleanTitle,
                                    url = if (href.startsWith("http")) href
                                    else "$BASE_URL${if (href.startsWith("/")) href else "/$href"}",
                                    category = directoryPath.substringAfterLast("/"),
                                    date = extractDate(text.ifEmpty { href }),
                                    fileSizeMb = 0.0
                                )
                            )
                        )
                    }
                }
            }

            // If no items found via anchors, try table-based parsing (fallback)
            if (items.isEmpty()) {
                items.addAll(parseTableListing(doc, directoryPath))
            }

            items.distinctBy {
                when (it) {
                    is BrowseItem.Folder -> it.path
                    is BrowseItem.Audio -> it.audioItem.id
                }
            }
        }

    private fun parseTableListing(doc: Document, currentPath: String): List<BrowseItem> {
        val items = mutableListOf<BrowseItem>()
        val tds = doc.select("td")
        for (td in tds) {
            val a = td.selectFirst("a[href]") ?: continue
            val href = a.attr("href")
            val text = a.text().trim()
            if (text.isEmpty()) continue

            if (href.contains("index.php?q=f&f=")) {
                val encoded = href.substringAfter("f=")
                val decoded = URLDecoder.decode(encoded, "UTF-8")
                if (!decoded.startsWith("$currentPath/")) continue
                val rel = decoded.removePrefix("$currentPath/")
                if (rel.isBlank() || rel.contains("/")) continue
                items.add(BrowseItem.Folder(cleanFolderName(text), decoded, 0))
            } else if (href.endsWith(".mp3", ignoreCase = true)) {
                val audioUrl = if (href.startsWith("http")) href else "$BASE_URL$href"
                val id = href.substringAfterLast("/").removeSuffix(".mp3")
                items.add(
                    BrowseItem.Audio(
                        AudioItem(
                            id = id,
                            title = cleanAudioTitle(text.ifEmpty { href.substringAfterLast("/") }),
                            url = audioUrl,
                            category = currentPath.substringAfterLast("/"),
                            date = extractDate(href)
                        )
                    )
                )
            }
        }
        return items
    }

    suspend fun getTopLevelCategories(): List<BrowseItem.Folder> = withContext(Dispatchers.IO) {
        ROOT_CATEGORIES.map { (displayName, folderName) ->
            BrowseItem.Folder(
                name = displayName,
                path = "$RADHANATH_LECTURES_PATH/$folderName",
                itemCount = 0
            )
        }
    }

    private fun extractItemCount(text: String): Int {
        val match = Regex("""(\d+)\s*(files?|folders?)""", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun cleanFolderName(raw: String): String {
        return raw
            .replace(Regex("""^\d+_-_"""), "")
            .replace("_", " ")
            .replace(Regex("""\s+\d+\s*(files?|folders?).*""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { raw }
    }

    private fun cleanAudioTitle(raw: String): String {
        return raw
            .replace(".mp3", "")
            .replace("_", " ")
            .replace(Regex("""^\d{4}-\d{3}_"""), "")
            .replace(Regex("""_-_Radhanath_Swami.*"""), "")
            .replace(Regex("""IDesireTree""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { raw }
    }

    private fun extractDate(text: String): String {
        val match = Regex("""\d{4}-\d{2}-\d{2}""").find(text)
        return match?.value ?: ""
    }
}
