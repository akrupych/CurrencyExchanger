package com.aeladrin.currencyexchanger.model

import com.aeladrin.currencyexchanger.ci.CurrenciesProvider
import com.aeladrin.currencyexchanger.ci.InitialBalancesProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class CurrencyExchangeRepositoryTest {

    private lateinit var repository: CurrencyExchangeRepository

    private val api = mockk<CurrencyExchangerApi>(relaxed = true)
    private val storage = mockk<CurrencyExchangeStorage>(relaxed = true)
    private val initialBalancesProvider = mockk<InitialBalancesProvider>(relaxed = true)
    private val currenciesProvider = mockk<CurrenciesProvider>(relaxed = true)

    private val balancesFlow = MutableStateFlow<Map<String, Double>?>(null)
    private val transactionsFlow = MutableStateFlow(0)

    @Before
    fun setup() {
        every { storage.balances } returns balancesFlow
        every { storage.transactions } returns transactionsFlow
        repository = CurrencyExchangeRepository(
            api = api,
            storage = storage,
            initialBalancesProvider = initialBalancesProvider,
            currenciesProvider = currenciesProvider,
        )
    }

    @Test
    fun getExchangeRatesSuccess() = runTest {
        val rates = mapOf("USD" to 1.16, "GBP" to 0.85, "JPY" to 130.0)
        coEvery { api.getExchangeRates() } returns ExchangeRates(base = "EUR", rates = rates)
        Assert.assertEquals(RatesResponse.Success(rates), repository.exchangeRates.first())
    }

    @Test
    fun getExchangeRatesError() = runTest {
        coEvery { api.getExchangeRates() } throws UnknownHostException()
        Assert.assertTrue(repository.exchangeRates.first() is RatesResponse.Error)
    }

    @Test
    fun getBalances() = runTest {
        coEvery { storage.setBalances(any()) } just Runs
        every { initialBalancesProvider.getInitialBalances() } returns mapOf("USD" to 100.0)
        Assert.assertEquals(mapOf("USD" to 100.0), repository.balances.first())
    }

    @Test
    fun getCurrencies() = runTest {
        every { currenciesProvider.getCurrencies() } returns listOf("USD", "EUR", "GBP")
        Assert.assertEquals(listOf("USD", "EUR", "GBP"), repository.currencies.first())
    }

    @Test
    fun getTransactions() = runTest {
        transactionsFlow.emit(5)
        Assert.assertEquals(5, repository.transactions.first())
    }

    @Test
    fun transactionMade() = runTest {
        repository.transactionMade(mapOf("USD" to 100.0))
        coVerify { storage.setBalances(mapOf("USD" to 100.0)) }
        coVerify { storage.setTransactions(1) }
    }
}