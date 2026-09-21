package com.example.sayvis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * SAYVIS v5.3.0 — the text assistant's refreshed face: Vazirmatn
 * (SIL OFL, bundled for all weights the chat uses). Persian-first design,
 * tall line-height for chat readability.
 */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

/** Chat text styles shared by ChatScreen / assistant bubbles. */
object SayvisChatType {
    /** Assistant (SAYVIS) message body. */
    val assistant = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 23.sp
    )

    /** Owner message body. */
    val owner = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )

    /** The «در حال فکر کردن …» status line (کد ۰۱). */
    val thinking = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
}

/** App typography with the assistant face applied to body sizes. */
val SayvisTypography: Typography = Typography().let { base ->
    base.copy(
        bodyLarge = base.bodyLarge.copy(fontFamily = Vazirmatn, lineHeight = 23.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = Vazirmatn, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontFamily = Vazirmatn, lineHeight = 18.sp),
        titleLarge = base.titleLarge.copy(fontFamily = Vazirmatn),
        titleMedium = base.titleMedium.copy(fontFamily = Vazirmatn),
        titleSmall = base.titleSmall.copy(fontFamily = Vazirmatn)
    )
}
