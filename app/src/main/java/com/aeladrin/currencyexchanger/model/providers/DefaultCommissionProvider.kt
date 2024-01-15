package com.aeladrin.currencyexchanger.model.providers

import com.aeladrin.currencyexchanger.ci.CommissionProvider
import com.aeladrin.currencyexchanger.model.CurrencyExchangeRepository
import com.aeladrin.currencyexchanger.utils.MoneyFormat
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DefaultCommissionProvider @Inject constructor(
    private val repository: CurrencyExchangeRepository,
) : CommissionProvider {

    override suspend fun getCommission(
        from: Pair<String, Double>,
        to: Pair<String, Double>
    ): Pair<Double, String> {
        val transactions = repository.transactions.firstOrNull() ?: 0
        return if (transactions < 5) {
            0.0 to ""
        } else {
            val commission = from.second * 0.007
            commission to "Commission Fee - ${MoneyFormat.format(commission)} ${from.first}"
        }
    }
}