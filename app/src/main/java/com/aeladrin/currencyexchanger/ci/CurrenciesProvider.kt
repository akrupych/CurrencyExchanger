package com.aeladrin.currencyexchanger.ci

interface CurrenciesProvider {
    fun getCurrencies(): List<String>
}