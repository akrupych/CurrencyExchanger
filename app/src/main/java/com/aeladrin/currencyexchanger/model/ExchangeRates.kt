package com.aeladrin.currencyexchanger.model

data class ExchangeRates(
    val base: String,
    val rates: Map<String, Double>,
)
