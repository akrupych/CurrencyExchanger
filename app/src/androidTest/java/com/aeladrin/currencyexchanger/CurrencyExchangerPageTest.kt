package com.aeladrin.currencyexchanger

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aeladrin.currencyexchanger.ui.CurrencyExchangerPage
import com.aeladrin.currencyexchanger.ui.CurrencyExchangerViewModel
import com.aeladrin.currencyexchanger.ui.CurrencyExchangerViewState
import com.aeladrin.currencyexchanger.ui.theme.CurrencyExchangerTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class CurrencyExchangerPageTest {

    @get:Rule
    val rule = createComposeRule()

    private val viewModel: CurrencyExchangerViewModel = mockk(relaxed = true)

    @Test
    fun testContent() {
        every { viewModel.viewState } returns MutableStateFlow(
            CurrencyExchangerViewState(
                balances = mapOf("USD" to 100.0, "EUR" to 90.0),
                sellCurrency = "USD",
                receiveAmount = 45.0,
                receiveCurrency = "EUR",
                error = "Network error",
            )
        )

        rule.setContent {
            CurrencyExchangerTheme {
                CurrencyExchangerPage(viewModel)
            }
        }

        rule.onNodeWithText("100.00 USD").assertIsDisplayed()
        rule.onNodeWithText("90.00 EUR").assertIsDisplayed()
        rule.onNodeWithText("+45.00").assertIsDisplayed()
        rule.onNodeWithText("USD").assertIsDisplayed()
        rule.onNodeWithText("EUR").assertIsDisplayed()
        rule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun testTypingSellAmount() {
        every { viewModel.viewState } returns MutableStateFlow(
            CurrencyExchangerViewState()
        )

        rule.setContent {
            CurrencyExchangerTheme {
                CurrencyExchangerPage(viewModel)
            }
        }
        rule.onNodeWithTag("SellAmount").performTextInput("123.45")

        verify { viewModel.onSellAmountChange(123.45) }
    }

    @Test
    fun testChangingSellCurrency() {
        every { viewModel.viewState } returns MutableStateFlow(
            CurrencyExchangerViewState(
                sellCurrency = "USD",
                sellCurrencyOptions = listOf("EUR"),
            )
        )

        rule.setContent {
            CurrencyExchangerTheme {
                CurrencyExchangerPage(viewModel)
            }
        }
        rule.onNodeWithText("USD").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("EUR").performClick()

        verify { viewModel.onSellCurrencyChange("EUR") }
    }

    @Test
    fun testChangingReceiveCurrency() {
        every { viewModel.viewState } returns MutableStateFlow(
            CurrencyExchangerViewState(
                receiveCurrency = "USD",
                receiveCurrencyOptions = listOf("EUR"),
            )
        )

        rule.setContent {
            CurrencyExchangerTheme {
                CurrencyExchangerPage(viewModel)
            }
        }
        rule.onNodeWithText("USD").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("EUR").performClick()

        verify { viewModel.onReceiveCurrencyChange("EUR") }
    }

    @Test
    fun testSubmitButtonClick() {
        every { viewModel.viewState } returns MutableStateFlow(
            CurrencyExchangerViewState()
        )

        rule.setContent {
            CurrencyExchangerTheme {
                CurrencyExchangerPage(viewModel)
            }
        }
        rule.onNodeWithText("SUBMIT").performClick()

        verify { viewModel.onSubmitClick() }
    }
}