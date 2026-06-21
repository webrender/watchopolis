package com.watchopolis.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.watchopolis.wear.engine.Evaluation
import com.watchopolis.wear.engine.MicropolisEngine

@Composable
fun EvaluationScreen(
    engine: MicropolisEngine,
    modifier: Modifier = Modifier,
) {
    // Read once when the screen opens.
    val eval = remember { engine.readEvaluation() }
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .crownScroll(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ListHeader { Text("Evaluation") }

        Stat("Approval", "${eval.approval}%", approvalColor(eval.approval))
        Stat("Score", "${eval.score}", scoreColor(eval.score), sub = signed(eval.scoreDelta))
        Stat("Class", eval.className, Color.White)

        Text(
            "Top problems",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (eval.problems.isEmpty()) {
            Text("None — nice work!", color = Color(0xFF9ED99E), fontSize = 12.sp)
        } else {
            eval.problems.forEach { (id, votes) ->
                Text(
                    "${Evaluation.problemName(id)}  $votes%",
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: Color, sub: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = Color.Gray, fontSize = 9.sp)
    }
}

private fun signed(v: Int): String = if (v >= 0) "+$v" else "$v"

private fun approvalColor(pct: Int): Color = when {
    pct >= 60 -> Color(0xFF34C759)
    pct >= 40 -> Color(0xFFE6C300)
    else -> Color(0xFFE5503A)
}

private fun scoreColor(score: Int): Color = when {
    score >= 600 -> Color(0xFF34C759)
    score >= 400 -> Color(0xFFE6C300)
    else -> Color(0xFFE5503A)
}
