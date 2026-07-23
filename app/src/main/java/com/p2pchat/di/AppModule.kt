package com.p2pchat.di

import com.p2pchat.data.repository.ChatRepositoryImpl
import com.p2pchat.data.repository.GroupRepositoryImpl
import com.p2pchat.data.repository.PeerRepositoryImpl
import com.p2pchat.domain.repository.ChatRepository
import com.p2pchat.domain.repository.GroupRepository
import com.p2pchat.domain.repository.PeerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-wide bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindPeerRepository(impl: PeerRepositoryImpl): PeerRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository
}
