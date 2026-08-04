package com.example.testapp004.di

import com.example.testapp004.data.ApkInstaller
import com.example.testapp004.data.GitHubUpdateRepository
import com.example.testapp004.data.OkHttpApkInstaller
import com.example.testapp004.data.UpdateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: GitHubUpdateRepository): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindApkInstaller(impl: OkHttpApkInstaller): ApkInstaller

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
    }
}
