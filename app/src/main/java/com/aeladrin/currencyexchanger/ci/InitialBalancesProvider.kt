package com.aeladrin.currencyexchanger.ci

interface InitialBalancesProvider {
    /**
     * @return currencies mapped to initial amounts
     */
    fun getInitialBalances(): Map<String, Double>
}