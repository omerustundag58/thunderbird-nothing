package com.fsck.k9.ui.localai

import androidx.annotation.StringRes
import com.fsck.k9.ui.R

/**
 * The virtual (local only) categories used by the on-device classifier.
 *
 * The [apiName] is the stable value written to the local cache and returned by the language model. The display name
 * is always resolved from resources so the UI follows the app language.
 */
enum class EmailCategory(
    val apiName: String,
    @StringRes val titleRes: Int,
) {
    FINANCE("finance", R.string.local_ai_category_finance),
    GAMING("gaming", R.string.local_ai_category_gaming),
    SUBSCRIPTION("subscription", R.string.local_ai_category_subscription),
    SHOPPING("shopping", R.string.local_ai_category_shopping),
    TRAVEL("travel", R.string.local_ai_category_travel),
    VERIFICATION("verification", R.string.local_ai_category_verification),
    SOCIAL("social", R.string.local_ai_category_social),
    NEWSLETTER("newsletter", R.string.local_ai_category_newsletter),
    OTHER("other", R.string.local_ai_category_other),
    ;

    companion object {
        /**
         * All category names the language model is allowed to answer with.
         */
        val apiNames: List<String> = entries.map { it.apiName }

        fun fromApiName(value: String?): EmailCategory? {
            if (value == null) return null
            return entries.firstOrNull { it.apiName == value }
        }
    }
}
