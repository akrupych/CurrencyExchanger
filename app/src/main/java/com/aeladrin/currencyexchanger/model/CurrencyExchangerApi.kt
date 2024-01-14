package com.aeladrin.currencyexchanger.model

import retrofit2.http.GET

interface CurrencyExchangerApi {

    companion object {
        const val BaseUrl = "https://developers.paysera.com/"
    }

    @GET("tasks/api/currency-exchange-rates")
    suspend fun getExchangeRates(): ExchangeRates
}