package com.example.testapp004.di

import com.example.testapp004.data.AndroidDriveAuthorizer
import com.example.testapp004.data.BackupLocalStore
import com.example.testapp004.data.BackupRemoteStore
import com.example.testapp004.data.BackupRepository
import com.example.testapp004.data.DriveAuthorizer
import com.example.testapp004.data.DriveBackupRemoteStore
import com.example.testapp004.data.DriveBackupRepository
import com.example.testapp004.data.RoomBackupLocalStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: DriveBackupRepository): BackupRepository

    @Binds
    @Singleton
    abstract fun bindDriveAuthorizer(impl: AndroidDriveAuthorizer): DriveAuthorizer

    @Binds
    @Singleton
    abstract fun bindBackupRemoteStore(impl: DriveBackupRemoteStore): BackupRemoteStore

    @Binds
    @Singleton
    abstract fun bindBackupLocalStore(impl: RoomBackupLocalStore): BackupLocalStore
}
