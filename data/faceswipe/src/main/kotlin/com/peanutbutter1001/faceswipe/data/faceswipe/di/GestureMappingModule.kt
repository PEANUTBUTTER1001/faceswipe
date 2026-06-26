package com.peanutbutter1001.faceswipe.data.faceswipe.di

import com.peanutbutter1001.faceswipe.data.faceswipe.repository.GestureMappingRepositoryImpl
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.GestureMappingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GestureMappingModule {

    @Binds
    @Singleton
    abstract fun bindGestureMappingRepository(
        impl: GestureMappingRepositoryImpl,
    ): GestureMappingRepository
}
