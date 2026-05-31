package com.example.testapp004.di

import com.example.testapp004.data.InMemoryNotesRepository
import com.example.testapp004.data.NotesRepository
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
    abstract fun bindNotesRepository(impl: InMemoryNotesRepository): NotesRepository
}
