package com.peanutbutter1001.faceswipe.di

import com.peanutbutter1001.faceswipe.data.repository.FaceTrackingRepositoryImpl
import com.peanutbutter1001.faceswipe.domain.repository.FaceTrackingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /**
     * FaceTrackingRepository 인터페이스를 FaceTrackingRepositoryImpl 싱글턴으로 바인딩.
     * AnalyzeFaceMovementUseCase 등 Domain 레이어에서 인터페이스를 통해 주입받음.
     */
    @Binds
    @Singleton
    abstract fun bindFaceTrackingRepository(
        impl: FaceTrackingRepositoryImpl
    ): FaceTrackingRepository
}
