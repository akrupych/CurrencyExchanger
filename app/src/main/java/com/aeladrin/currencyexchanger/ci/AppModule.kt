package com.aeladrin.currencyexchanger.ci

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideCurrencies(): CurrenciesProvider {
        return object : CurrenciesProvider {
            override fun getCurrencies(): List<String> {
                return listOf("EUR", "USD", "BGN", "UAH")
            }
        }
    }

    @Provides
    fun provideInitialBalances(): InitialBalancesProvider {
        return object : InitialBalancesProvider {
            override fun getInitialBalances(): Map<String, Double> {
                return mapOf("EUR" to 1000.0)
            }
        }
    }
}

interface CurrenciesProvider {
    fun getCurrencies(): List<String>
}

interface InitialBalancesProvider {
    /**
     * @return currencies mapped to initial amounts
     */
    fun getInitialBalances(): Map<String, Double>
}