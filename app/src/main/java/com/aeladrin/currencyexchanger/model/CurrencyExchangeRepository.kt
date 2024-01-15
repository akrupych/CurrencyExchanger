package com.aeladrin.currencyexchanger.model

import com.aeladrin.currencyexchanger.ci.CurrenciesProvider
import com.aeladrin.currencyexchanger.ci.InitialBalancesProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CurrencyExchangeRepository @Inject constructor(
    private val api: CurrencyExchangerApi,
    private val storage: CurrencyExchangeStorage,
    private val initialBalancesProvider: InitialBalancesProvider,
    private val currenciesProvider: CurrenciesProvider,
) {
    val exchangeRates: Flow<RatesResponse> = flow {
        var rates: ExchangeRates? = null
        while (true) {
            try {
                val newRates = api.getExchangeRates()
                if (newRates != rates) {
                    rates = newRates
                    emit(RatesResponse.Success(rates.rates))
                }
            } catch (e: Exception) {
                emit(RatesResponse.Error("Exchange rates are unavailable at the moment"))
            }
            delay(5000)
        }
    }

    val balances: Flow<Map<String, Double>> = storage.balances.map {
        it ?: initialBalancesProvider.getInitialBalances().apply { storage.setBalances(this) }
    }

    // we could add new currencies here dynamically
    val currencies: Flow<List<String>> = flow {
        emit(currenciesProvider.getCurrencies())
    }

    val transactions: Flow<Int> = storage.transactions

    suspend fun transactionMade(newBalances: Map<String, Double>) {
        storage.setBalances(newBalances)
        storage.setTransactions(storage.transactions.first() + 1)
    }
}

sealed class RatesResponse {
    data class Success(val rates: Map<String, Double>) : RatesResponse()
    data class Error(val message: String) : RatesResponse()
}