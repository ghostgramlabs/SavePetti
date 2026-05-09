package com.savepetti.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Quieter radii. The app used to lean into 24/32dp surfaces and full-pill
// chips, which read as "Figma starter kit". Pulled everything back to feel
// closer to a stationery notebook — gently rounded, not perfectly pill.
val SavePettiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)
