package com.watchopolis.wear.engine

/**
 * City budget snapshot. Funding percents are 0..1; the actual yearly spend for a
 * service is its fund amount times its percent.
 */
data class Budget(
    val tax: Int,
    val funds: Long,
    val taxIncome: Long,
    val roadFund: Long,
    val fireFund: Long,
    val policeFund: Long,
    val roadPercent: Float,
    val firePercent: Float,
    val policePercent: Float,
) {
    val roadSpend: Long get() = (roadFund * roadPercent).toLong()
    val fireSpend: Long get() = (fireFund * firePercent).toLong()
    val policeSpend: Long get() = (policeFund * policePercent).toLong()
    val totalSpend: Long get() = roadSpend + fireSpend + policeSpend
    val net: Long get() = taxIncome - totalSpend
}
