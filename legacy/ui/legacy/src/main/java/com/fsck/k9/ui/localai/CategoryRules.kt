package com.fsck.k9.ui.localai

import java.util.Locale

data class RuleClassification(
    val category: EmailCategory,
    val score: Int,
    val ambiguous: Boolean,
)

/**
 * Cheap, fully offline first pass.
 *
 * Rules look at the sender domain and the subject. Turkish and English keywords are both included and matching is
 * done on a small ASCII normalized copy so "Doğrulama" and "dogrulama" behave the same. When the score is weak or
 * tied the result is marked ambiguous so the caller can consult the local model (or fall back to "other").
 */
object CategoryRules {
    private const val DOMAIN_WEIGHT = 5
    private const val MAX_SCORE = 12

    private val domainHints: Map<EmailCategory, List<String>> = mapOf(
        EmailCategory.FINANCE to listOf(
            "garanti", "akbank", "isbank", "yapikredi", "ziraat", "vakifbank", "halkbank", "qnb", "denizbank",
            "enpara", "papara", "paypal", "stripe", "adyen", "wise.com", "revolut", "binance", "coinbase",
            "kraken", "kuveytturk", "ingbank", "hsbc", "citibank", "americanexpress", "amex", "mastercard",
            "visa.com", "financial", "bank",
        ),
        EmailCategory.GAMING to listOf(
            "steampowered", "steamcommunity", "playstation", "xbox", "nintendo", "epicgames", "riotgames",
            "blizzard", "battle.net", "garena", "supercell", "ubisoft", "ea.com", "rockstargames", "roblox",
            "twitch", "gamepass", "humblebundle",
        ),
        EmailCategory.SUBSCRIPTION to listOf(
            "netflix", "spotify", "disneyplus", "hbomax", "primevideo", "youtube", "adobe", "dropbox", "icloud",
            "apple.com", "google.com", "microsoft", "notion.so", "canva", "openai", "anthropic", "patreon",
            "onlyfans", "audible", "kindle",
        ),
        EmailCategory.SHOPPING to listOf(
            "amazon", "trendyol", "hepsiburada", "n11", "aliexpress", "ebay", "etsy", "shein", "temu", "zara",
            "hm.com", "decathlon", "ikea", "mediamarkt", "teknosa", "vatanbilgisayar", "lcwaikiki", "boyner",
            "gratis", "watsons", "getir", "yemeksepeti", "migros", "shopify",
        ),
        EmailCategory.TRAVEL to listOf(
            "booking", "airbnb", "expedia", "trivago", "trip.com", "skyscanner", "pegasus", "thy.com",
            "turkishairlines", "sunexpress", "anadolujet", "uber", "bolt.eu", "lyft", "obilet", "ets.com",
            "enuygun", "hotels.com", "marriott", "hilton", "ryanair", "easyjet",
        ),
        EmailCategory.VERIFICATION to listOf(
            "auth0", "okta", "twilio", "verify", "identity", "onelogin",
        ),
        EmailCategory.SOCIAL to listOf(
            "facebook", "instagram", "linkedin", "twitter", "x.com", "tiktok", "discord", "reddit", "telegram",
            "pinterest", "snapchat", "whatsapp", "threads.net", "mastodon", "bsky",
        ),
        EmailCategory.NEWSLETTER to listOf(
            "substack", "mailchimp", "mailerlite", "buttondown", "medium.com", "beehiiv", "convertkit", "ghost.io",
            "revue", "campaign-archive",
        ),
    )

    private val subjectKeywords: Map<EmailCategory, List<Pair<String, Int>>> = mapOf(
        EmailCategory.FINANCE to listOf(
            "ekstre" to 4, "hesap ozeti" to 4, "hesap hareketi" to 3, "kredi karti" to 4, "kredi" to 2,
            "banka" to 2, "havale" to 3, "eft" to 2, "odemeniz" to 2, "odeme" to 1, "fatura" to 2,
            "invoice" to 2, "payment" to 2, "statement" to 3, "balance" to 2, "bakiye" to 2,
            "islem bildirimi" to 3, "transaction" to 2, "taksit" to 2, "faiz" to 2, "borc" to 2,
            "finans" to 3,
        ),
        EmailCategory.GAMING to listOf(
            "oyun" to 2, "game" to 2, "steam" to 4, "playstation" to 4, "xbox" to 4, "nintendo" to 4,
            "epic games" to 4, "riot games" to 4, "battle pass" to 3, "skin" to 2, "oyun kodu" to 3,
            "game pass" to 3, "psn" to 3, "esports" to 3, "konsol" to 2,
        ),
        EmailCategory.SUBSCRIPTION to listOf(
            "abonelik" to 4, "subscription" to 4, "uyelik" to 3, "membership" to 3, "yenilendi" to 3,
            "renewal" to 3, "yenileme" to 3, "renew" to 2, "deneme suresi" to 3, "free trial" to 3,
            "trial" to 2, "planiniz" to 2, "plan" to 1, "otomatik odeme" to 4, "aylik ucret" to 3,
            "fatura" to 2,
        ),
        EmailCategory.SHOPPING to listOf(
            "siparis" to 4, "order" to 4, "kargo" to 4, "shipping" to 3, "teslimat" to 4, "delivery" to 3,
            "satin alma" to 3, "satin" to 2, "purchase" to 3, "sepet" to 3, "cart" to 2, "indirim" to 2,
            "discount" to 2, "kampanya" to 2, "urun" to 1, "product" to 1, "iade" to 3, "refund" to 3,
            "stok" to 2, "kupon" to 2, "coupon" to 2, "firsat" to 1,
        ),
        EmailCategory.TRAVEL to listOf(
            "ucus" to 4, "flight" to 4, "otel" to 4, "hotel" to 4, "rezervasyon" to 4, "reservation" to 4,
            "seyahat" to 3, "travel" to 3, "bilet" to 3, "ticket" to 2, "boarding" to 4, "check-in" to 3,
            "check in" to 2, "pnr" to 4, "tatil" to 3, "holiday" to 2, "rent a car" to 3,
            "arac kiralama" to 3, "ucus kodu" to 3,
        ),
        EmailCategory.VERIFICATION to listOf(
            "dogrulama" to 5, "verification" to 5, "verify" to 4, "otp" to 5, "one-time" to 4,
            "one time" to 3, "tek kullanimlik" to 5, "giris kodu" to 5, "login code" to 5,
            "security code" to 4, "guvenlik kodu" to 5, "kodunuz" to 4, "code is" to 3, "code:" to 2,
            "pin" to 2, "2fa" to 5, "iki adimli" to 5, "aktivasyon" to 3, "activation" to 3,
            "sifre sifirlama" to 5, "password reset" to 5, "reset your password" to 5, "dogrulama kodu" to 5,
        ),
        EmailCategory.SOCIAL to listOf(
            "arkadaslik" to 4, "friend request" to 4, "bahsetti" to 3, "mentioned you" to 4, "yorum" to 2,
            "comment" to 2, "begendi" to 3, "liked your" to 4, "takip" to 3, "follow" to 2, "bildirim" to 2,
            "notification" to 2, "mesaj gonderdi" to 3, "sent you a message" to 4, "davet" to 2,
            "invitation" to 2, "baglanti istegi" to 4, "connection request" to 4, "sosyal" to 3,
        ),
        EmailCategory.NEWSLETTER to listOf(
            "bulten" to 5, "newsletter" to 5, "haftalik" to 3, "weekly" to 3, "digest" to 4, "ozet" to 2,
            "unsubscribe" to 5, "abonelikten cik" to 5, "bu e-postayi" to 4, "you are receiving this" to 4,
            "issue #" to 3, "sayi" to 1, "gorusuruz" to 1,
        ),
        EmailCategory.OTHER to emptyList(),
    )

    private val oneTimeCodePattern = Regex("\\b\\d{4,8}\\b")
    private val verificationWords = listOf("kod", "code", "otp", "pin", "sifre", "password")

    fun classify(
        subject: String?,
        sender: String?,
        hasUnsubscribeHeader: Boolean = false,
    ): RuleClassification {
        val normalizedSubject = normalize(subject.orEmpty())
        val normalizedSender = normalize(sender.orEmpty())
        val domain = normalizedSender.substringAfterLast('@', normalizedSender)

        val scores = mutableMapOf<EmailCategory, Int>()
        for (category in EmailCategory.entries) {
            var score = 0

            val hints = domainHints[category].orEmpty()
            if (hints.any { hint -> matchesDomain(domain, hint) }) {
                score += DOMAIN_WEIGHT
            }

            val keywords = subjectKeywords[category].orEmpty()
            for ((keyword, weight) in keywords) {
                if (containsKeyword(normalizedSubject, keyword)) {
                    score += weight
                }
            }

            if (category == EmailCategory.VERIFICATION && score > 0) {
                val hasCode = oneTimeCodePattern.containsMatchIn(normalizedSubject)
                val mentionsCode = verificationWords.any { containsKeyword(normalizedSubject, it) }
                if (hasCode && mentionsCode) {
                    score += 6
                }
            }

            if (category == EmailCategory.NEWSLETTER && hasUnsubscribeHeader) {
                score += 4
            }

            if (score > 0) {
                scores[category] = score.coerceAtMost(MAX_SCORE)
            }
        }

        if (scores.isEmpty()) {
            return RuleClassification(
                category = EmailCategory.OTHER,
                score = 0,
                ambiguous = true,
            )
        }

        val sorted = scores.entries.sortedByDescending { it.value }
        val best = sorted.first()
        val secondScore = sorted.getOrNull(1)?.value ?: 0
        val ambiguous = best.value < 2 || best.value == secondScore

        return RuleClassification(
            category = best.key,
            score = best.value,
            ambiguous = ambiguous,
        )
    }

    private fun containsKeyword(text: String, keyword: String): Boolean {
        var index = text.indexOf(keyword)
        while (index >= 0) {
            val before = text.getOrNull(index - 1)
            val after = text.getOrNull(index + keyword.length)
            val startsClean = before == null || !before.isLetterOrDigit()
            val endsClean = after == null || !after.isLetterOrDigit()
            if (startsClean && endsClean) return true
            index = text.indexOf(keyword, index + 1)
        }
        return false
    }

    private fun matchesDomain(domain: String, hint: String): Boolean {
        if (domain == hint) return true
        if (domain.startsWith("$hint.") || domain.endsWith(".$hint")) return true
        // Short hints ("bank", "visa") must match a whole domain label; longer
        // brand fragments ("garanti", "hepsiburada") may appear inside a label.
        return hint.length >= 6 && domain.contains(hint)
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.ROOT)
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
            .replace("i̇", "i")
    }
}
