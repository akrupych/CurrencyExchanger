package com.aeladrin.currencyexchanger.ci

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.aeladrin.currencyexchanger.BuildConfig
import com.aeladrin.currencyexchanger.model.CurrencyExchangeRepository
import com.aeladrin.currencyexchanger.model.CurrencyExchangeStorage
import com.aeladrin.currencyexchanger.model.CurrencyExchangerApi
import com.aeladrin.currencyexchanger.model.providers.DefaultCommissionProvider
import com.aeladrin.currencyexchanger.model.providers.DefaultCurrenciesProvider
import com.aeladrin.currencyexchanger.model.providers.DefaultInitialBalancesProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "storage")

    @Provides
    fun provideCurrencies(): CurrenciesProvider = DefaultCurrenciesProvider()

    @Provides
    fun provideInitialBalances(): InitialBalancesProvider = DefaultInitialBalancesProvider()

    @Provides
    fun provideCommissionProvider(repository: CurrencyExchangeRepository): CommissionProvider =
        DefaultCommissionProvider(repository)

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
    fun provideCurrencyExchangeStorage(
        @ApplicationContext appContext: Context
    ): CurrencyExchangeStorage = CurrencyExchangeStorage(appContext.dataStore)

    @Provides
    fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
}