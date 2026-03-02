package com.idk.anypay.ui.theme

import androidx.compose.ui.graphics.Color

// ── shadcn/ui Design Tokens ──────────────────────────────────────────────────
// Base palette – Zinc (neutral, clean, shadcn default)

// Zinc scale
val Zinc50  = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc600 = Color(0xFF52525B)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)
val Zinc900 = Color(0xFF18181B)
val Zinc950 = Color(0xFF09090B)

// White / Black
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// ── Semantic / Accent colors (shadcn default accent = near-black / white) ────
// Primary action – charcoal (matches shadcn "primary" in light mode)
val ShadcnPrimary       = Color(0xFF18181B)   // zinc-900
val ShadcnPrimaryFg     = Color(0xFFFAFAFA)   // zinc-50

// Destructive
val ShadcnDestructive   = Color(0xFFEF4444)   // red-500
val ShadcnDestructiveFg = Color(0xFFFEF2F2)   // red-50

// Muted
val ShadcnMuted         = Color(0xFFF4F4F5)   // zinc-100
val ShadcnMutedFg       = Color(0xFF71717A)   // zinc-500

// Accent (hover / subtle highlight)
val ShadcnAccent        = Color(0xFFF4F4F5)   // zinc-100
val ShadcnAccentFg      = Color(0xFF18181B)   // zinc-900

// Border / Input / Ring
val ShadcnBorder        = Color(0xFFE4E4E7)   // zinc-200
val ShadcnInput         = Color(0xFFE4E4E7)
val ShadcnRing          = Color(0xFF18181B)

// ── Dark mode versions ───────────────────────────────────────────────────────
val ShadcnPrimaryDark       = Color(0xFFFAFAFA)  // zinc-50
val ShadcnPrimaryFgDark     = Color(0xFF18181B)  // zinc-900
val ShadcnMutedDark         = Color(0xFF27272A)  // zinc-800
val ShadcnMutedFgDark       = Color(0xFFA1A1AA)  // zinc-400
val ShadcnAccentDark        = Color(0xFF27272A)  // zinc-800
val ShadcnAccentFgDark      = Color(0xFFFAFAFA)  // zinc-50
val ShadcnBorderDark        = Color(0xFF27272A)  // zinc-800
val ShadcnInputDark         = Color(0xFF27272A)

// ── Status / Transaction colors ──────────────────────────────────────────────
val SendRed           = Color(0xFFEF4444)   // red-500
val ReceiveGreen      = Color(0xFF22C55E)   // green-500
val BalanceBlue       = Color(0xFF3B82F6)   // blue-500
val PendingAmber      = Color(0xFFF59E0B)   // amber-500
val SuccessGreen      = Color(0xFF22C55E)
val ErrorRed          = Color(0xFFEF4444)
val WarningYellow     = Color(0xFFF59E0B)

// ── Category colors ──────────────────────────────────────────────────────────
val CategoryFood          = Color(0xFFF97316)  // orange-500
val CategoryShopping      = Color(0xFF8B5CF6)  // violet-500
val CategoryGrocery       = Color(0xFF10B981)  // emerald-500
val CategoryTransport     = Color(0xFF06B6D4)  // cyan-500
val CategoryEntertainment = Color(0xFFEC4899)  // pink-500
val CategoryBills         = Color(0xFFEF4444)  // red-500
val CategoryHealth        = Color(0xFF22C55E)  // green-500
val CategoryEducation     = Color(0xFF3B82F6)  // blue-500
val CategoryPersonal      = Color(0xFFA855F7)  // purple-500
val CategoryOther         = Color(0xFF71717A)  // zinc-500

// Keep legacy aliases so existing code compiles without changes
val PendingOrange = PendingAmber
val Blue700 = Color(0xFF1565C0)
val Blue600 = Color(0xFF1E88E5)
val Blue500 = Color(0xFF2196F3)
val Blue100 = Color(0xFFBBDEFB)
val Blue50  = Color(0xFFE3F2FD)
val Gray50  = Zinc50
val Gray100 = Zinc100
val Gray200 = Zinc200
val Gray300 = Zinc300
val Gray400 = Zinc400
val Gray500 = Zinc500
val Gray600 = Zinc600
val Gray700 = Zinc700
val Gray750 = Color(0xFF323238)
val Gray800 = Zinc800
val Gray850 = Color(0xFF1F1F22)
val Gray900 = Zinc900
