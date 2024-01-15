package com.aeladrin.currencyexchanger.ci

interface CommissionProvider {
    /**
     * Calculate commission for currency exchange
     * @param from sell currency and amount
     * @param to receive currency and amount
     * @return commission amount and message
     */
    suspend fun getCommission(from: Pair<String, Double>, to: Pair<String, Double>): Pair<Double, String>
}