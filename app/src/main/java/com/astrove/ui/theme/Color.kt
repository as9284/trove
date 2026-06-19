package com.astrove.ui.theme

import androidx.compose.ui.graphics.Color

// Editorial palette (see branding/BRAND.md). Brass is a spice, never a fill
// (the one exception is the single primary action per screen).

// Core brand
val Ink = Color(0xFF1B1A17)
val Paper = Color(0xFFF2ECDF)
val Brass = Color(0xFFC0892E)

// Light-theme derivations (surfaces built down from Paper)
val Linen = Color(0xFFFBF7EE)            // inputs and cards on paper
val PaperDim = Color(0xFFE7E0D0)         // muted surfaces, thumbnail wells
val InkMuted = Color(0xFF6F6A5E)         // secondary text on paper
val HairlineLight = Color(0xFFCDC4B0)    // borders on paper
val HairlineLightSoft = Color(0xFFDED7C6)

// Dark-theme derivations (surfaces built up from Ink). Kept deliberately warm
// (red ≥ green ≥ blue) so neutrals never read cool/purple next to the brass.
val InkRaised = Color(0xFF23201A)        // cards on ink
val InkElevated = Color(0xFF2D2A21)      // thumbnail wells on ink
val InkSoft = Color(0xFF453F30)          // hairlines on ink
val PaperOnInk = Color(0xFFEDE7DA)       // primary text on ink
val PaperMutedOnInk = Color(0xFFA8A294)  // secondary text on ink
