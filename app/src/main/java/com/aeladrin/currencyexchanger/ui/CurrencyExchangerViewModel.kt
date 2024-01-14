package com.aeladrin.currencyexchanger.ui

import androidx.lifecycle.ViewModel
import com.aeladrin.currencyexchanger.ci.CurrenciesProvider
import com.aeladrin.currencyexchanger.ci.InitialBalancesProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CurrencyExchangerViewModel @Inject constructor(
    private val currenciesProvider: CurrenciesProvider,
    private val initialBalancesProvider: InitialBalancesProvider,
) : ViewModel() {

    private val _viewState = MutableStateFlow(CurrencyExchangerViewState())
    val viewState: StateFlow<CurrencyExchangerViewState> = _viewState.asStateFlow()

    fun loadData() {
        val currencies = currenciesProvider.getCurrencies()
        val balances = initialBalancesProvider.getInitialBalances()
        _viewState.update { state ->
            state.copy(
                balances = currencies.associateWith { (balances[it] ?: 0.0) },
                sellCurrencies = currencies,
                receiveCurrencies = currencies.drop(1),
            )
        }
    }

    fun onSellCurrencyChange(currency: String) {
        val currencies = currenciesProvider.getCurrencies()
        _viewState.update { state ->
            state.copy(
                sellCurrencies = listOf(currency) + currencies.filterNot { it == currency },
                receiveCurrencies = currencies.filterNot { it == currency },
            )
        }
        // TODO: update receiveMoney
    }

    fun onReceiveCurrencyChange(currency: String) {
        val currencies = currenciesProvider.getCurrencies()
        _viewState.update { state ->
            state.copy(
                receiveCurrencies = listOf(currency) + currencies.filterNot {
                    it == currency || it == state.sellCurrencies.first()
                },
            )
        }
        // TODO: update receiveMoney
    }

    fun onSubmitClick() {
        // TODO: apply conversion
    }

    fun onSellAmountChange(amount: Double) {
        // TODO: update receiveMoney
    }
}

data class CurrencyExchangerViewState(
    val balances: Map<String, Double> = emptyMap(), // currency to amount map
    val sellAmount: Double = 0.0,
    val sellCurrencies: List<String> = emptyList(), // selected currency is first
    val receiveAmount: Double = 0.0,
    val receiveCurrencies: List<String> = emptyList(), // selected currency is first
)
