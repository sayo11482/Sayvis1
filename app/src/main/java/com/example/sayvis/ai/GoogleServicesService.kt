package com.example.sayvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Read-only access to the signed-in owner's Google account capabilities:
 * Gmail, Google Calendar and Google Drive — exactly the scopes consented to
 * during the [GoogleAuthManager] sign-in. Every call is read-only, audited by
 * the caller, and never throws (failures become bilingual messages).
 */
class GoogleServicesService {

    data class FetchResult(
        val ok: Boolean,
        val title: String,
        val lines: List<String>,
        val messageFa: String = "",
        val messageEn: String = ""
    )

    /** Which Google capability a normalized prompt asks about. */
    enum class Capability { GMAIL, CALENDAR, DRIVE }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    companion object {

        private val GMAIL_WORDS = listOf("ایمیل", "میل", "نامه", "جیمیل", "gmail", "email", "mail")
        private val CALENDAR_WORDS = listOf("تقویم", "رویداد", "جلسه", "قرار", "calendar", "event", "meeting")
        private val DRIVE_WORDS = listOf("درایو", "فایل‌هایم", "فایل هایم", "اسناد گوگل", "drive", "gdrive")

        /** Returns the capability the text asks about, or null. */
        fun matchCapability(normalizedText: String): Capability? = when {
            GMAIL_WORDS.any { normalizedText.contains(it) } -> Capability.GMAIL
            CALENDAR_WORDS.any { normalizedText.contains(it) } -> Capability.CALENDAR
            DRIVE_WORDS.any { normalizedText.contains(it) } -> Capability.DRIVE
            else -> null
        }
    }

    /** Recent inbox threads — optionally filtered by a plain Gmail search query. */
    suspend fun recentEmails(accessToken: String, searchQuery: String?): FetchResult =
        withContext(Dispatchers.IO) {
            val listUrl = buildString {
                append("https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=5")
                if (!searchQuery.isNullOrBlank()) append("&q=").append(urlencode(searchQuery))
            }
            val listResponse = getJson(accessToken, listUrl)
                ?: return@withContext networkFailure("جیمیل", "Gmail")

            if (!listResponse.ok) return@withContext listResponse

            val ids = runCatching {
                val arr = JSONObject(listResponse.title).optJSONArray("messages")
                (0 until (arr?.length() ?: 0)).mapNotNull { arr?.optJSONObject(it)?.optString("id") }
            }.getOrDefault(emptyList())

            if (ids.isEmpty()) {
                return@withContext FetchResult(
                    true, "", emptyList(),
                    "ایمیلی مطابق درخواست پیدا نشد",
                    "No email matched the request"
                )
            }

            val lines = ArrayList<String>()
            for (id in ids.take(3)) {
                val detail = getJson(
                    accessToken,
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata" +
                        "&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date"
                ) ?: continue
                if (!detail.ok) continue
                lines += describeEmail(detail.title)
            }
            if (lines.isEmpty()) networkFailure("جیمیل", "Gmail") else FetchResult(true, "", lines)
        }

    private fun describeEmail(messageJson: String): String {
        val json = runCatching { JSONObject(messageJson) }.getOrNull() ?: return "• (email)"
        var from = ""
        var subject = ""
        var date = ""
        val payload = json.optJSONObject("payload")
        val headerArray = payload?.optJSONArray("headers")
        if (headerArray != null) {
            for (i in 0 until headerArray.length()) {
                val header = headerArray.optJSONObject(i) ?: continue
                when (header.optString("name")) {
                    "From" -> from = header.optString("value")
                    "Subject" -> subject = header.optString("value")
                    "Date" -> date = header.optString("value")
                }
            }
        }
        val snippet = json.optString("snippet").take(90)
        return buildString {
            append("• ")
            if (subject.isNotBlank()) append(subject)
            if (from.isNotBlank()) append(" — ").append(from)
            if (date.isNotBlank()) append(" (").append(date).append(")")
            if (snippet.isNotBlank()) append("\n   ").append(snippet)
        }
    }

    /** Upcoming calendar events (next 7 days). */
    suspend fun upcomingEvents(accessToken: String): FetchResult = withContext(Dispatchers.IO) {
        val timeMin = java.time.Instant.now().toString()
        val response = getJson(
            accessToken,
            "https://www.googleapis.com/calendar/v3/users/me/events?timeMin=" + urlencode(timeMin) +
                "&maxResults=6&singleEvents=true&orderBy=startTime"
        ) ?: return@withContext networkFailure("تقویم", "Calendar")
        if (!response.ok) return@withContext response

        val items = runCatching {
            JSONObject(response.title).optJSONArray("items")
        }.getOrNull()
        val lines = ArrayList<String>()
        val count = items?.length() ?: 0
        for (i in 0 until count) {
            val item = items?.optJSONObject(i) ?: continue
            val summary = item.optString("summary").ifBlank { "(بدون عنوان)" }
            val start = item.optJSONObject("start")?.optString("dateTime").ifBlankOrNull()
                ?: item.optJSONObject("start")?.optString("date").ifBlankOrNull()
                ?: ""
            lines += "• $summary${if (start.isNotBlank()) " — $start" else ""}"
        }
        if (lines.isEmpty()) {
            FetchResult(true, "", emptyList(), "رویداد پیش‌رویی پیدا نشد", "No upcoming events found")
        } else FetchResult(true, "", lines)
    }

    /** Google Drive file search by name. */
    suspend fun findFiles(accessToken: String, nameQuery: String?): FetchResult =
        withContext(Dispatchers.IO) {
            val q = if (nameQuery.isNullOrBlank()) ""
            else urlencode("name contains '" + nameQuery.replace("'", "") + "' and trashed=false")
            val response = getJson(
                accessToken,
                "https://www.googleapis.com/drive/v3/files?pageSize=6&fields=files(name,mimeType,modifiedTime)" +
                    (if (q.isNotBlank()) "&q=$q" else "")
            ) ?: return@withContext networkFailure("درایو", "Drive")
            if (!response.ok) return@withContext response

            val files = runCatching {
                JSONObject(response.title).optJSONArray("files")
            }.getOrNull()
            val lines = ArrayList<String>()
            val count = files?.length() ?: 0
            for (i in 0 until count) {
                val file = files?.optJSONObject(i) ?: continue
                lines += "• ${file.optString("name")} (${file.optString("mimeType")})"
            }
            if (lines.isEmpty()) {
                FetchResult(true, "", emptyList(), "فایلی پیدا نشد", "No files found")
            } else FetchResult(true, "", lines)
        }

    // ---------------------------------------------------------------- helpers

    /** GET with bearer token; ok=true carries the raw JSON in [FetchResult.title]. */
    private fun getJson(accessToken: String, url: String): FetchResult? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            when {
                response.isSuccessful -> FetchResult(true, body, emptyList())
                response.code == 401 || response.code == 403 -> FetchResult(
                    false, "", emptyList(),
                    "دسترسی گوگل منقضی یا رد شد؛ دوباره وارد شوید",
                    "Google access expired or denied; sign in again"
                )
                else -> FetchResult(
                    false, "", emptyList(),
                    "گوگل کد ${response.code} برگرداند",
                    "Google returned ${response.code}"
                )
            }
        }
    }.getOrNull()

    private fun networkFailure(fa: String, en: String) = FetchResult(
        false, "", emptyList(),
        "$fa در دسترس نیست (شبکه/تحریم). VPN یا «آزمودن اتصال» را امتحان کنید.",
        "$en is unreachable (network/region block). Try a VPN or “Test connection”."
    )

    private fun urlencode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private fun String?.ifBlankOrNull(): String? = if (this.isNullOrBlank()) null else this
}
