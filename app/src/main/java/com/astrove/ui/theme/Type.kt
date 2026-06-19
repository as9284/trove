@file:OptIn(ExperimentalTextApi::class)

package com.astrove.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.astrove.R

// Fraunces (SIL OFL) bundled as a variable font (see branding/BRAND.md).
// We pin weight 500 and choose the optical size per role: high contrast for big
// display type, a calmer cut for smaller headings. Body/labels stay on the
// system sans for legibility.

private fun fraunces(weight: Int, opticalSize: Float) = Font(
    resId = R.font.fraunces,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.opticalSizing(opticalSize.sp),
    ),
)

private val FrauncesDisplay = FontFamily(
    fraunces(weight = 500, opticalSize = 144f),
    fraunces(weight = 400, opticalSize = 144f),
)

private val FrauncesHeading = FontFamily(
    fraunces(weight = 500, opticalSize = 40f),
    fraunces(weight = 400, opticalSize = 28f),
)

private val base = Typography()

val TroveTypography = base.copy(
    displayLarge = base.displayLarge.copy(fontFamily = FrauncesDisplay, fontWeight = FontWeight.Medium),
    displayMedium = base.displayMedium.copy(fontFamily = FrauncesDisplay, fontWeight = FontWeight.Medium),
    displaySmall = base.displaySmall.copy(fontFamily = FrauncesDisplay, fontWeight = FontWeight.Medium),
    headlineLarge = base.headlineLarge.copy(fontFamily = FrauncesHeading, fontWeight = FontWeight.Medium),
    headlineMedium = base.headlineMedium.copy(fontFamily = FrauncesHeading, fontWeight = FontWeight.Medium),
    headlineSmall = base.headlineSmall.copy(fontFamily = FrauncesHeading, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontFamily = FrauncesHeading, fontWeight = FontWeight.Medium),
)
