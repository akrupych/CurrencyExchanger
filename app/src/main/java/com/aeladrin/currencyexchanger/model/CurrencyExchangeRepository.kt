package com.aeladrin.currencyexchanger.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aeladrin.currencyexchanger.ci.CurrenciesProvider
import com.aeladrin.currencyexchanger.ci.InitialBalancesProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CurrencyExchangeRepository @Inject constructor(
    private val api: CurrencyExchangerApi,
    private val dataStore: DataStore<Preferences>,
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

    val balances: Flow<Map<String, Double>> = dataStore.data.map { prefs ->
        val type = object: TypeToken<Map<String, Double>>(){}.type
        prefs[stringPreferencesKey("balances")]?.let { Gson().fromJson(it, type) }
            ?: initialBalancesProvider.getInitialBalances().apply { setBalances(this) }
    }

    suspend fun setBalances(balances: Map<String, Double>) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("balances")] = Gson().toJson(balances)
        }
    }

    // we could add new currencies here dynamically
    val currencies: Flow<List<String>> = flow {
        emit(currenciesProvider.getCurrencies())
    }
}

sealed class RatesResponse {
    data class Success(val rates: Map<String, Double>) : RatesResponse()
    data class Error(val message: String) : RatesResponse()
}