package com.aeladrin.currencyexchanger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aeladrin.currencyexchanger.ui.CurrencyExchangerPage
import com.aeladrin.currencyexchanger.ui.theme.CurrencyExchangerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CurrencyExchangerTheme {
                CurrencyExchangerPage()
            }
        }
    }
}