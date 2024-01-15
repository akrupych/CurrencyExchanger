package com.aeladrin.currencyexchanger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeladrin.currencyexchanger.model.CurrencyExchangeRepository
import com.aeladrin.currencyexchanger.model.RatesResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyExchangerViewModel @Inject constructor(
    private val repository: CurrencyExchangeRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow(CurrencyExchangerViewState())
    val viewState: StateFlow<CurrencyExchangerViewState> = _viewState.asStateFlow()

    private var exchangeRates: Map<String, Double>? = null
    private var currencies: List<String>? = null

    init {
        viewModelScope.launch {
            repository.exchangeRates
                .combine(repository.balances) { rates, balances ->
                    rates to balances
                }
                .combine(repository.currencies) { (rates, balances), currencies ->
                    Triple(rates, balances, currencies)
                }
                .catch { exception -> _viewState.update { it.copy(error = exception.message) } }
                .collect { (rates, balances, currencies) ->
                    if (rates is RatesResponse.Success) exchangeRates = rates.rates
                    this@CurrencyExchangerViewModel.currencies = currencies
                    _viewState.update { state ->
                        val sellCurrency = state.sellCurrency.ifEmpty {
                            currencies.getOrNull(0) ?: ""
                        }
                        val sellCurrencyOptions = state.sellCurrencyOptions.ifEmpty {
                            currencies.drop(1)
                        }
                        val receiveCurrency = state.receiveCurrency.ifEmpty {
                            currencies.getOrNull(1) ?: ""
                        }
                        val receiveCurrencyOptions = state.receiveCurrencyOptions.ifEmpty {
                            currencies.drop(2)
                        }
                        val receiveAmount = if (rates is RatesResponse.Success) {
                            convert(
                                amount = state.sellAmount,
                                fromCurrency = sellCurrency,
                                toCurrency = receiveCurrency,
                                exchangeRates = rates.rates,
                            )
                        } else {
                            state.receiveAmount
                        }
                        val error = if (rates is RatesResponse.Error) rates.message else null
                        state.copy(
                            balances = currencies.associateWith { (balances[it] ?: 0.0) },
                            sellCurrency = sellCurrency,
                            sellCurrencyOptions = sellCurrencyOptions,
                            receiveCurrency = receiveCurrency,
                            receiveCurrencyOptions = receiveCurrencyOptions,
                            receiveAmount = receiveAmount,
                            error = error,
                        )
                    }
                }
        }
    }

    fun onSellCurrencyChange(currency: String) {
        val currencies = this.currencies ?: return
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
        val currencies = this.currencies ?: return
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
        exchangeRates: Map<String, Double>,
    ): Double {
        val fromRate = exchangeRates[fromCurrency] ?: return 0.0
        val toRate = exchangeRates[toCurrency] ?: return 0.0
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
    val error: String? = null,
)
