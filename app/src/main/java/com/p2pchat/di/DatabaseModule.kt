package com.p2pchat.di

import android.content.Context
import androidx.room.Room
import com.p2pchat.data.local.ChatDatabase
import com.p2pchat.data.local.GroupDao
import com.p2pchat.data.local.MeshDao
import com.p2pchat.data.local.MessageDao
import com.p2pchat.data.local.PeerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "p2chat.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: ChatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun providePeerDao(database: ChatDatabase): PeerDao {
        return database.peerDao()
    }

    @Provides
    @Singleton
    fun provideGroupDao(database: ChatDatabase): GroupDao {
        return database.groupDao()
    }

    @Provides
    @Singleton
    fun provideMeshDao(database: ChatDatabase): MeshDao {
        return database.meshDao()
    }
}
