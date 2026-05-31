package com.example.testapp004.di

import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.InMemoryAcquaintanceRepository
import com.example.testapp004.data.InMemoryCategoryRepository
import com.example.testapp004.data.InMemoryRelationRepository
import com.example.testapp004.data.RelationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: InMemoryCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAcquaintanceRepository(impl: InMemoryAcquaintanceRepository): AcquaintanceRepository

    @Binds
    @Singleton
    abstract fun bindRelationRepository(impl: InMemoryRelationRepository): RelationRepository
}
