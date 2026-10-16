package dev.prateekthakur.devlens.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val steps = listOf(
    "reading AndroidManifest.xml",
    "classifying permissions",
    "checking exported components",
    "verifying signing certificate",
    "scanning strings for secrets",
    "decoding DEX classes",
    "building size report",
)

/** Terminal-style progress log, centered in its parent. */
@Composable
fun AnalyzingAnimation(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    var shown by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            if (shown < steps.size) shown++
        }
    }
    val cursor by rememberInfiniteTransition(label = "cursor").animateFloat(0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "blink")

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
            steps.take(shown).forEachIndexed { i, line ->
                val last = i == shown - 1
                Text(
                    (if (last) "[..] " else "[ok] ") + line + if (last && cursor > 0.5f) " _" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (last) cs.onSurface else cs.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
            )
        }
    }
}
