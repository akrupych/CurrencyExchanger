package com.aeladrin.currencyexchanger.ui

import com.aeladrin.currencyexchanger.ci.CommissionProvider
import com.aeladrin.currencyexchanger.model.CurrencyExchangeRepository
import com.aeladrin.currencyexchanger.model.RatesResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyExchangerViewModelTest {

    private lateinit var viewModel: CurrencyExchangerViewModel

    private val repository = mockk<CurrencyExchangeRepository>(relaxed = true)
    private val commissionProvider = mockk<CommissionProvider>(relaxed = true)

    private val exchangeRatesFlow = MutableSharedFlow<RatesResponse>()
    private val balancesFlow = MutableSharedFlow<Map<String, Double>>()
    private val currenciesFlow = MutableSharedFlow<List<String>>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.exchangeRates } returns exchangeRatesFlow
        every { repository.balances } returns balancesFlow
        every { repository.currencies } returns currenciesFlow

        viewModel = CurrencyExchangerViewModel(
            repository = repository,
            commissionProvider = commissionProvider,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun defaultViewState() = runTest {
        Assert.assertEquals(CurrencyExchangerViewState(), viewModel.viewState.value)
    }

    @Test
    fun initViewState() = runTest {
        exchangeRatesFlow.emit(RatesResponse.Success(mapOf("USD" to 1.129031, "EUR" to 1.0)))
        balancesFlow.emit(mapOf("USD" to 100.0, "EUR" to 200.0))
        currenciesFlow.emit(listOf("USD", "EUR"))

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("USD" to 100.0, "EUR" to 200.0),
                sellCurrency = "USD",
                sellCurrencyOptions = listOf("EUR"),
                receiveCurrency = "EUR",
                receiveCurrencyOptions = emptyList(),
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun errorViewState() = runTest {
        exchangeRatesFlow.emit(RatesResponse.Error("No internet connection"))
        balancesFlow.emit(mapOf("USD" to 100.0, "EUR" to 200.0))
        currenciesFlow.emit(listOf("USD", "EUR"))

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("USD" to 100.0, "EUR" to 200.0),
                sellCurrency = "USD",
                sellCurrencyOptions = listOf("EUR"),
                receiveCurrency = "EUR",
                receiveCurrencyOptions = emptyList(),
                error = "No internet connection",
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun onSellCurrencyChange() = runTest {
        exchangeRatesFlow.emit(RatesResponse.Success(mapOf("USD" to 1.129031, "EUR" to 1.0)))
        balancesFlow.emit(mapOf("USD" to 100.0, "EUR" to 200.0))
        currenciesFlow.emit(listOf("USD", "EUR"))

        viewModel.onSellCurrencyChange("EUR")

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("USD" to 100.0, "EUR" to 200.0),
                sellCurrency = "EUR",
                sellCurrencyOptions = listOf("USD"),
                receiveCurrency = "USD",
                receiveCurrencyOptions = emptyList(),
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun onReceiveCurrencyChange() = runTest {
        exchangeRatesFlow.emit(
            RatesResponse.Success(
                mapOf("USD" to 1.129031, "EUR" to 1.0, "GBP" to 0.835342)
            )
        )
        balancesFlow.emit(mapOf("USD" to 100.0, "EUR" to 200.0))
        currenciesFlow.emit(listOf("USD", "EUR", "GBP"))

        viewModel.onReceiveCurrencyChange("GBP")

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("USD" to 100.0, "EUR" to 200.0, "GBP" to 0.0),
                sellCurrency = "USD",
                sellCurrencyOptions = listOf("EUR", "GBP"),
                receiveCurrency = "GBP",
                receiveCurrencyOptions = listOf("EUR"),
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun onSellAmountChange() = runTest {
        exchangeRatesFlow.emit(RatesResponse.Success(mapOf("USD" to 1.129031, "EUR" to 1.0)))
        balancesFlow.emit(mapOf("EUR" to 100.0, "USD" to 200.0))
        currenciesFlow.emit(listOf("EUR", "USD"))

        viewModel.onSellAmountChange(100.0)

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("EUR" to 100.0, "USD" to 200.0),
                sellAmount = 100.0,
                sellCurrency = "EUR",
                sellCurrencyOptions = listOf("USD"),
                receiveAmount = 112.9031,
                receiveCurrency = "USD",
                receiveCurrencyOptions = emptyList(),
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun onSubmitClickSuccess() = runTest {
        exchangeRatesFlow.emit(RatesResponse.Success(mapOf("USD" to 1.129031, "EUR" to 1.0)))
        balancesFlow.emit(mapOf("EUR" to 100.0, "USD" to 0.0))
        currenciesFlow.emit(listOf("EUR", "USD"))
        coEvery { commissionProvider.getCommission(any(), any()) } returns
                Pair(0.0, "No commission.")
        val balancesSlot = slot<Map<String, Double>>()
        coEvery { repository.transactionMade(capture(balancesSlot)) } coAnswers {
            balancesFlow.emit(balancesSlot.captured)
        }

        viewModel.onSellAmountChange(100.0)
        viewModel.onSubmitClick()

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("EUR" to 0.0, "USD" to 112.9031),
                sellAmount = 100.0,
                sellCurrency = "EUR",
                sellCurrencyOptions = listOf("USD"),
                receiveAmount = 112.9031,
                receiveCurrency = "USD",
                receiveCurrencyOptions = emptyList(),
                dialogMessage = "You have converted 100.00 EUR to 112.90 USD. No commission."
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun onSubmitClickError() = runTest {
        exchangeRatesFlow.emit(RatesResponse.Success(mapOf("USD" to 1.129031, "EUR" to 1.0)))
        balancesFlow.emit(mapOf("EUR" to 100.0, "USD" to 0.0))
        currenciesFlow.emit(listOf("EUR", "USD"))
        coEvery { commissionProvider.getCommission(any(), any()) } returns
                Pair(0.01, "Commission Fee - 1.00 EUR.")
        val balancesSlot = slot<Map<String, Double>>()
        coEvery { repository.transactionMade(capture(balancesSlot)) } coAnswers {
            balancesFlow.emit(balancesSlot.captured)
        }

        viewModel.onSellAmountChange(100.0)
        viewModel.onSubmitClick()

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("EUR" to 100.0, "USD" to 0.0),
                sellAmount = 100.0,
                sellCurrency = "EUR",
                sellCurrencyOptions = listOf("USD"),
                receiveAmount = 112.9031,
                receiveCurrency = "USD",
                receiveCurrencyOptions = emptyList(),
                error = "Conversion failed, please check your balances",
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun onDismissDialog() = runTest {
        exchangeRatesFlow.emit(RatesResponse.Success(mapOf("USD" to 1.129031, "EUR" to 1.0)))
        balancesFlow.emit(mapOf("EUR" to 100.0, "USD" to 0.0))
        currenciesFlow.emit(listOf("EUR", "USD"))
        coEvery { commissionProvider.getCommission(any(), any()) } returns
                Pair(0.0, "No commission.")
        val balancesSlot = slot<Map<String, Double>>()
        coEvery { repository.transactionMade(capture(balancesSlot)) } coAnswers {
            balancesFlow.emit(balancesSlot.captured)
        }

        viewModel.onSellAmountChange(100.0)
        viewModel.onSubmitClick()
        viewModel.onDismissDialog()

        Assert.assertEquals(
            CurrencyExchangerViewState(
                balances = mapOf("EUR" to 0.0, "USD" to 112.9031),
                sellAmount = 100.0,
                sellCurrency = "EUR",
                sellCurrencyOptions = listOf("USD"),
                receiveAmount = 112.9031,
                receiveCurrency = "USD",
                receiveCurrencyOptions = emptyList(),
            ),
            viewModel.viewState.value,
        )
    }
}