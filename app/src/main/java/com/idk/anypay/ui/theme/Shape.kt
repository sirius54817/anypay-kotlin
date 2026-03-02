package com.idk.anypay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * shadcn/ui shape scale – slightly rounded, clean geometry.
 * shadcn uses radius-md (6px) as default; we scale for touch targets.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // badges, chips
    small      = RoundedCornerShape(6.dp),   // buttons, inputs
    medium     = RoundedCornerShape(8.dp),   // cards
    large      = RoundedCornerShape(12.dp),  // bottom sheets, dialogs
    extraLarge = RoundedCornerShape(16.dp)   // full-screen modals
)
