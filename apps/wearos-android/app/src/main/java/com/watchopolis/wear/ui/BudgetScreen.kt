package com.watchopolis.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.watchopolis.wear.engine.MicropolisEngine
import kotlin.math.roundToInt

@Composable
fun BudgetScreen(
    engine: MicropolisEngine,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var budget by remember { mutableStateOf(engine.readBudget()) }
    fun reload() {
        budget = engine.readBudget()
        onChanged()
    }

    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .crownScroll(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ListHeader { Text("Budget") }

        Text("Funds  $${compact(budget.funds)}", color = Color.White, fontSize = 13.sp)

        StepperRow(
            label = "Tax",
            value = "${budget.tax}%",
            onMinus = { engine.setTax((budget.tax - 1).coerceIn(0, 20)); reload() },
            onPlus = { engine.setTax((budget.tax + 1).coerceIn(0, 20)); reload() },
        )
        Text(
            "Tax income  $${compact(budget.taxIncome)}/yr",
            color = Color(0xFF9ED99E),
            fontSize = 11.sp,
        )

        FundingRow("Road", budget.roadPercent, budget.roadSpend, budget.roadFund) { pct ->
            engine.setFunding(pct, budget.firePercent, budget.policePercent); reload()
        }
        FundingRow("Fire", budget.firePercent, budget.fireSpend, budget.fireFund) { pct ->
            engine.setFunding(budget.roadPercent, pct, budget.policePercent); reload()
        }
        FundingRow("Police", budget.policePercent, budget.policeSpend, budget.policeFund) { pct ->
            engine.setFunding(budget.roadPercent, budget.firePercent, pct); reload()
        }

        val net = budget.net
        Text(
            "Net  ${if (net >= 0) "+" else "-"}$${compact(kotlin.math.abs(net))}/yr",
            color = if (net >= 0) Color(0xFF34C759) else Color(0xFFE5503A),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepButton("−", onMinus)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 12.sp)
        }
        StepButton("+", onPlus)
    }
}

@Composable
private fun FundingRow(
    label: String,
    percent: Float,
    spend: Long,
    fund: Long,
    onSet: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepButton("−") { onSet((percent - 0.1f).coerceIn(0f, 1f)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$label  ${(percent * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("$${compact(spend)} / $${compact(fund)}", color = Color.Gray, fontSize = 10.sp)
        }
        StepButton("+") { onSet((percent + 0.1f).coerceIn(0f, 1f)) }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2A2A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Color.White, fontSize = 20.sp)
    }
}
