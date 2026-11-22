package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// VYBE Signature Palette — Obsidian Twilight + Electric Coral + Luminous Violet
val VybeObsidian = Color(0xFF0E0C16)
val VybeSurface = Color(0xFF151223)
val VybeSurfaceElevated = Color(0xFF1D1930)
val VybeSurfaceHigh = Color(0xFF282341)
val VybeOutlineDark = Color(0xFF3A3359)

val VybeCoral = Color(0xFFFF4D6D)
val VybeCoralDark = Color(0xFF3E0A18)
val VybeViolet = Color(0xFF8A6BFF)
val VybeVioletDark = Color(0xFF251A52)
val VybeAmber = Color(0xFFFFB547)
val VybeAmberDark = Color(0xFF3D2705)
val VybeMint = Color(0xFF2EE59D)

val VybeInkLight = Color(0xFFF6F4FF)
val VybeMutedLight = Color(0xFFB3ACC9)

// Light Theme Tokens
val VybeCream = Color(0xFFFAF8FF)
val VybeLightSurface = Color(0xFFFFFFFF)
val VybeLightCard = Color(0xFFF2EFFA)
val VybeLightHigh = Color(0xFFE7E2F5)
val VybeOutlineLight = Color(0xFFD0C9E6)
val VybeInkDark = Color(0xFF151126)
val VybeMutedDark = Color(0xFF5D557A)
val VybeCoralDeep = Color(0xFFD92B50)
val VybeVioletDeep = Color(0xFF5E3EE0)
val VybeAmberDeep = Color(0xFFAA6800)

fun avatarAccentColor(token: String): Color = when (token) {
    "coral" -> VybeCoral
    "violet" -> VybeViolet
    "amber" -> VybeAmber
    "mint" -> VybeMint
    else -> VybeCoral
}
