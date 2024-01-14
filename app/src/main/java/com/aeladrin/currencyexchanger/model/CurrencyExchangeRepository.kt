package com.aeladrin.currencyexchanger.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CurrencyExchangeRepository @Inject constructor(
    private val api: CurrencyExchangerApi,
) {
    val exchangeRates: Flow<ExchangeRates> = flow {
        var rates: ExchangeRates? = null
        while (true) {
            val newRates = api.getExchangeRates()
            if (newRates != rates) {
                rates = newRates
                emit(newRates)
            }
            delay(5000)
        }
    }
}