package com.aeladrin.currencyexchanger.model.providers

import com.aeladrin.currencyexchanger.ci.InitialBalancesProvider

class DefaultInitialBalancesProvider : InitialBalancesProvider {
    override fun getInitialBalances(): Map<String, Double> {
        return mapOf("EUR" to 1000.0)
    }
}