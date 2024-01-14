package com.aeladrin.currencyexchanger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeladrin.currencyexchanger.ci.CurrenciesProvider
import com.aeladrin.currencyexchanger.ci.InitialBalancesProvider
import com.aeladrin.currencyexchanger.model.CurrencyExchangeRepository
import com.aeladrin.currencyexchanger.model.ExchangeRates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyExchangerViewModel @Inject constructor(
    private val currenciesProvider: CurrenciesProvider,
    private val initialBalancesProvider: InitialBalancesProvider,
    private val repository: CurrencyExchangeRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow(CurrencyExchangerViewState())
    val viewState: StateFlow<CurrencyExchangerViewState> = _viewState.asStateFlow()

    private var exchangeRates: ExchangeRates? = null

    init {
        val currencies = currenciesProvider.getCurrencies()
        val balances = initialBalancesProvider.getInitialBalances()
        _viewState.value = CurrencyExchangerViewState(
            balances = currencies.associateWith { (balances[it] ?: 0.0) },
            sellCurrency = currencies.getOrNull(0) ?: "",
            sellCurrencyOptions = currencies.drop(1),
            receiveCurrency = currencies.getOrNull(1) ?: "",
            receiveCurrencyOptions = currencies.drop(2),
        )
        viewModelScope.launch {
            repository.exchangeRates.collect { rates ->
                    exchangeRates = rates
                    _viewState.update { state ->
                        state.copy(
                            receiveAmount = convert(
                                amount = state.sellAmount,
                                fromCurrency = state.sellCurrency,
                                toCurrency = state.receiveCurrency,
                                exchangeRates = rates,
                            ),
                        )
                    }
                }
        }
    }

    fun onSellCurrencyChange(currency: String) {
        val currencies = currenciesProvider.getCurrencies()
        val options = currencies.filterNot { it == currency }
        val newState = _viewState.value.copy(
            sellCurrency = currency,
            sellCurrencyOptions = options,
            receiveCurrency = options.firstOrNull() ?: "",
            receiveCurrencyOptions = options.drop(1),
        )
        _viewState.value = newState.copy(
            receiveAmount = exchangeRates?.let {
                convert(
                    amount = newState.sellAmount,
                    fromCurrency = newState.sellCurrency,
                    toCurrency = newState.receiveCurrency,
                    exchangeRates = it,
                )
            } ?: 0.0,
        )
    }

    fun onReceiveCurrencyChange(currency: String) {
        val currencies = currenciesProvider.getCurrencies()
        val newState = _viewState.value.copy(
            receiveCurrency = currency,
            receiveCurrencyOptions = currencies.filterNot {
                it == currency || it == _viewState.value.sellCurrency
            },
        )
        _viewState.value = newState.copy(
            receiveAmount = exchangeRates?.let {
                convert(
                    amount = newState.sellAmount,
                    fromCurrency = newState.sellCurrency,
                    toCurrency = newState.receiveCurrency,
                    exchangeRates = it,
                )
            } ?: 0.0,
        )
    }

    fun onSellAmountChange(amount: Double) {
        _viewState.update { state ->
            state.copy(
                sellAmount = amount,
                receiveAmount = exchangeRates?.let {
                    convert(
                        amount = amount,
                        fromCurrency = state.sellCurrency,
                        toCurrency = state.receiveCurrency,
                        exchangeRates = it,
                    )
                } ?: 0.0,
            )
        }
    }

    fun onSubmitClick() {
        // TODO: apply conversion
    }

    private fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: ExchangeRates,
    ): Double {
        val fromRate = exchangeRates.rates[fromCurrency] ?: return 0.0
        val toRate = exchangeRates.rates[toCurrency] ?: return 0.0
        return amount * toRate / fromRate
    }
}

data class CurrencyExchangerViewState(
    val balances: Map<String, Double> = emptyMap(), // currency to amount map
    val sellAmount: Double = 0.0,
    val sellCurrency: String = "",
    val sellCurrencyOptions: List<String> = emptyList(),
    val receiveAmount: Double = 0.0,
    val receiveCurrency: String = "",
    val receiveCurrencyOptions: List<String> = emptyList(),
)
