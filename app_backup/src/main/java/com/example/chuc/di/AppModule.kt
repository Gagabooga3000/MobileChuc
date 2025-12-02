package com.example.chuc.di

import android.content.Context
import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.local.ParserPreferences
import com.example.chuc.data.network.PersistentCookieJar
import com.example.chuc.data.network.NetworkModule
import com.example.chuc.data.network.WebService
import com.example.chuc.data.repository.ScheduleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthPreferences(@ApplicationContext context: Context): AuthPreferences {
        return AuthPreferences(context)
    }

    @Provides
    @Singleton
    fun provideParserPreferences(@ApplicationContext context: Context): ParserPreferences {
        return ParserPreferences(context)
    }

    @Provides
    @Singleton
    fun providePersistentCookieJar(): PersistentCookieJar {
        return PersistentCookieJar()
    }

    @Provides
    @Singleton
    fun provideCookieJar(cookieJar: PersistentCookieJar): CookieJar {
        return cookieJar
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(cookieJar: CookieJar): OkHttpClient {
        return NetworkModule.provideOkHttpClient(cookieJar)
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return NetworkModule.provideRetrofit(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideWebService(retrofit: Retrofit): WebService {
        return NetworkModule.provideWebService(retrofit)
    }

    @Provides
    @Singleton
    fun provideScheduleRepository(webService: WebService, authPreferences: AuthPreferences): ScheduleRepository {
        return ScheduleRepository(webService, authPreferences)
    }
}