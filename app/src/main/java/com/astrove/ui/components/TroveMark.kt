package com.astrove.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrove.ui.theme.TroveTheme

/**
 * Trove's mark: four screenshot crop brackets framing a brass gem.
 * Brackets take [bracketColor] (defaults to the current content ink); the gem
 * is always the brass primary.
 */
@Composable
fun TroveMark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    bracketColor: Color = MaterialTheme.colorScheme.onBackground,
    gemColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val m = s * 0.10f
        val arm = s * 0.26f
        val strokeW = s * 0.085f
        val stroke = Stroke(width = strokeW, cap = StrokeCap.Butt, join = StrokeJoin.Miter)

        fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
            val p = Path().apply {
                moveTo(cx + dx * arm, cy)
                lineTo(cx, cy)
                lineTo(cx, cy + dy * arm)
            }
            drawPath(p, color = bracketColor, style = stroke)
        }

        corner(m, m, 1f, 1f)               // top-left
        corner(s - m, m, -1f, 1f)          // top-right
        corner(s - m, s - m, -1f, -1f)     // bottom-right
        corner(m, s - m, 1f, -1f)          // bottom-left

        val c = s / 2f
        val r = s * 0.20f
        val gem = Path().apply {
            moveTo(c, c - r)
            lineTo(c + r, c)
            lineTo(c, c + r)
            lineTo(c - r, c)
            close()
        }
        drawPath(gem, color = gemColor)
    }
}

@Preview
@Composable
private fun TroveMarkPreview() {
    TroveTheme { TroveMark(size = 64.dp) }
}
