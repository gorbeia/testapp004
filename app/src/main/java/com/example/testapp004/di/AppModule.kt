package com.example.testapp004.di

import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.AndroidContactRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.ContactRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.data.RoomAcquaintanceRepository
import com.example.testapp004.data.RoomCategoryRepository
import com.example.testapp004.data.RoomRelationRepository
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
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAcquaintanceRepository(impl: RoomAcquaintanceRepository): AcquaintanceRepository

    @Binds
    @Singleton
    abstract fun bindRelationRepository(impl: RoomRelationRepository): RelationRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: AndroidContactRepository): ContactRepository
}
