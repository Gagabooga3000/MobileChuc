package com.example.chuc.di

import android.content.Context
import com.example.chuc.VkApiClient
import com.example.chuc.VkNewsParser
import com.example.chuc.data.local.AuthPreferences
import com.example.chuc.data.local.ParserPreferences
import com.example.chuc.data.mediator.DebugLoginInterceptor
import com.example.chuc.data.mediator.MediatorApiService
import com.example.chuc.data.mediator.MediatorRepository
import com.example.chuc.data.network.PersistentCookieJar
import com.example.chuc.data.network.NetworkModule
import com.example.chuc.data.network.WebService
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
    fun provideDebugLoginInterceptor(authPreferences: AuthPreferences): DebugLoginInterceptor {
        return DebugLoginInterceptor(authPreferences)
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
    @Named("main")
    fun provideMainRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return NetworkModule.provideRetrofit(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideWebService(@Named("main") retrofit: Retrofit): WebService {
        return NetworkModule.provideWebService(retrofit)
    }

    @Provides
    @Singleton
    fun provideVkApiClient(): VkApiClient {
        return VkApiClient()
    }

    @Provides
    @Singleton
    fun provideVkNewsParser(): VkNewsParser {
        return VkNewsParser()
    }

    @Provides
    @Singleton
    @Named("mediator")
    fun provideMediatorOkHttpClient(
        cookieJar: CookieJar,
        debugLoginInterceptor: DebugLoginInterceptor
    ): OkHttpClient {
        return NetworkModule.provideMediatorOkHttpClient(cookieJar, debugLoginInterceptor)
    }

    @Provides
    @Singleton
    @Named("mediator")
    fun provideMediatorRetrofit(@Named("mediator") okHttpClient: OkHttpClient): Retrofit {
        return NetworkModule.provideMediatorRetrofit(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideMediatorApiService(@Named("mediator") retrofit: Retrofit): MediatorApiService {
        return NetworkModule.provideMediatorApiService(retrofit)
    }

    @Provides
    @Singleton
    fun provideMediatorRepository(api: MediatorApiService): MediatorRepository {
        return MediatorRepository(api)
    }
}