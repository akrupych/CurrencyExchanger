package com.aeladrin.currencyexchanger.ci

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.aeladrin.currencyexchanger.BuildConfig
import com.aeladrin.currencyexchanger.model.CurrencyExchangerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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

    @Provides
    fun provideCurrencyExchangerApi(): CurrencyExchangerApi {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(CurrencyExchangerApi.BaseUrl)
            .client(
                OkHttpClient.Builder()
                    .readTimeout(5, TimeUnit.SECONDS)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                            else HttpLoggingInterceptor.Level.NONE
                        }
                    )
                    .build()
            )
            .build()
            .create(CurrencyExchangerApi::class.java)
    }

    @Provides
    fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return appContext.dataStore
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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "storage")