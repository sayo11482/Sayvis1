package com.example.sayvis.agent

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

/**
 * Device IO half of the manager agent (v5.0.0):
 *
 *  - reads the SMS inbox through the real Telephony provider (READ_SMS);
 *  - resolves the contact display name for each sender (READ_CONTACTS,
 *    already granted at first launch);
 *  - classifies every message with the pure [BusinessDirectory] rules and
 *    keeps the strongest business signal per sender;
 *  - Instagram profiles are classified from the owner-provided bio and
 *    context (Instagram's own API needs a business token, so the honest
 *    on-device path is the bio/text the owner supplies — the classifier
 *    and directory are exactly the same as SMS).
 */
class SmsDirectoryAgent {

    data class RawSms(val address: String, val body: String, val date: Long)

    /** Real inbox scan; returns parsed SMS sorted by business score desc. */
    fun scanInbox(context: Context): List<Pair<RawSms, BusinessDirectory.Classification>> {
        val rows = ArrayList<RawSms>()
        runCatching {
            val uri = Uri.parse("content://sms/inbox")
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf("address", "body", "date"),
                null,
                null,
                "date DESC"
            )
            cursor?.use { c ->
                val addrIdx = c.getColumnIndexOrThrow("address")
                val bodyIdx = c.getColumnIndexOrThrow("body")
                val dateIdx = c.getColumnIndexOrThrow("date")
                while (c.moveToNext() && rows.size < 2000) {
                    rows.add(
                        RawSms(
                            address = c.getString(addrIdx).orEmpty(),
                            body = c.getString(bodyIdx).orEmpty(),
                            date = c.getLong(dateIdx)
                        )
                    )
                }
            }
        }
        return rows.map { it to BusinessDirectory.classify(it.body) }
            .filter { it.second.score >= 16 }
            .sortedByDescending { it.second.score }
    }

    /** Contact display name for a phone number (empty when not in contacts). */
    fun contactName(context: Context, phone: String): String = runCatching {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { c ->
            if (c.moveToFirst()) c.getString(0).orEmpty() else ""
        } ?: ""
    }.getOrDefault("")

    /**
     * Full scan → directory entries. Each business sender becomes one entry;
     * the contact name gives first/family, the strongest SMS gives
     * company/site/address and the category.
     */
    fun buildEntries(context: Context): List<BusinessDirectory.Entry> {
        val scored = scanInbox(context)
        val bestPerSender = LinkedHashMap<String, Pair<RawSms, BusinessDirectory.Classification>>()
        for (pair in scored) {
            val key = pair.first.address.trim()
            val existing = bestPerSender[key]
            if (existing == null || pair.second.score > existing.second.score) {
                bestPerSender[key] = pair
            }
        }
        val now = System.currentTimeMillis()
        return bestPerSender.entries.map { (address, pair) ->
            val (sms, cls) = pair
            val display = contactName(context, address)
            val (given, family) = BusinessDirectory.splitPersonName(display)
            val text = sms.body
            BusinessDirectory.Entry(
                name = if (given.isBlank() && BusinessDirectory.isSenderCode(address)) address else given,
                family = family,
                phone = address,
                company = BusinessDirectory.extractCompany(text),
                category = cls.category,
                site = BusinessDirectory.extractWebsite(text),
                address = BusinessDirectory.extractAddress(text),
                signals = cls.signals,
                provenance = "sms",
                score = cls.score,
                scannedAt = now
            )
        }
    }

    /**
     * Instagram intake → the same directory entry shape. `bio` is the account
     * biography text (owner-pasted or captured), `context` optionally carries
     * "followers=..;following=..;posts=.." to enrich the signals.
     */
    fun instagramEntry(
        handle: String,
        bio: String,
        context: String,
        now: Long
    ): BusinessDirectory.Entry? {
        val h = handle.trim().removePrefix("@")
        if (h.isBlank() || bio.isBlank()) return null
        val cls = BusinessDirectory.classify(bio + " " + context)
        if (cls.category == BusinessDirectory.Category.OTHER) return null
        return BusinessDirectory.Entry(
            name = "@" + h,
            family = "",
            phone = "instagram:" + h.lowercase(),
            company = BusinessDirectory.extractCompany(bio),
            category = cls.category,
            site = BusinessDirectory.extractWebsite(bio),
            address = BusinessDirectory.extractAddress(bio),
            signals = cls.signals,
            provenance = "instagram",
            score = cls.score,
            scannedAt = now
        )
    }
}
