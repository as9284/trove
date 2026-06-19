package com.astrove.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

/** Motion tokens: fast and crisp, with a slight physical settle on small interactions. */
object TroveMotion {
    const val EnterMs = 200
    const val ExitMs = 150

    /** Spring for small interactive elements (pins, checkmarks). */
    fun <T> spec(): SpringSpec<T> = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Default screen enter: a quick fade with a slight scale. */
    val ScreenEnter: EnterTransition =
        fadeIn(tween(EnterMs)) + scaleIn(initialScale = 0.97f, animationSpec = tween(EnterMs))

    val ScreenExit: ExitTransition = fadeOut(tween(ExitMs))

    val ScreenPopEnter: EnterTransition = fadeIn(tween(EnterMs))

    val ScreenPopExit: ExitTransition =
        fadeOut(tween(ExitMs)) + scaleOut(targetScale = 0.97f, animationSpec = tween(ExitMs))

    /** Detail grows in and collapses back (shared-element style). */
    val DetailEnter: EnterTransition =
        fadeIn(tween(EnterMs)) + scaleIn(initialScale = 0.92f, animationSpec = tween(EnterMs))

    val DetailPopExit: ExitTransition =
        fadeOut(tween(ExitMs)) + scaleOut(targetScale = 0.92f, animationSpec = tween(ExitMs))
}
