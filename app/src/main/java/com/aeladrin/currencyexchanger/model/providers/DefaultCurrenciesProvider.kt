package com.aeladrin.currencyexchanger.model.providers

import com.aeladrin.currencyexchanger.ci.CurrenciesProvider

class DefaultCurrenciesProvider : CurrenciesProvider {
    override fun getCurrencies(): List<String> {
        return listOf("EUR", "USD", "BGN", "UAH")
    }
}