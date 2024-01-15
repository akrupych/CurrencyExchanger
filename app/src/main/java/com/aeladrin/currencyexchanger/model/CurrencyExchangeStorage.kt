package com.aeladrin.currencyexchanger.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CurrencyExchangeStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        const val KeyBalances = "balances"
        const val KeyTransactions = "transactions"
    }

    val balances: Flow<Map<String, Double>?> = dataStore.data.map { prefs ->
        val type = object: TypeToken<Map<String, Double>>(){}.type
        prefs[stringPreferencesKey(KeyBalances)]?.let { Gson().fromJson(it, type) }
    }

    suspend fun setBalances(balances: Map<String, Double>) {
        dataStore.edit { it[stringPreferencesKey(KeyBalances)] = Gson().toJson(balances) }
    }

    val transactions: Flow<Int> = dataStore.data.map {
        it[intPreferencesKey(KeyTransactions)] ?: 0
    }

    suspend fun setTransactions(transactions: Int) {
        dataStore.edit { it[intPreferencesKey(KeyTransactions)] = transactions }
    }
}