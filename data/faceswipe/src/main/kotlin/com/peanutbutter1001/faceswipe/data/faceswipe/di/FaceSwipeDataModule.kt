package com.peanutbutter1001.faceswipe.data.faceswipe.di

import com.peanutbutter1001.faceswipe.data.faceswipe.repository.FaceTrackingRepositoryImpl
import com.peanutbutter1001.faceswipe.data.faceswipe.service.LifecycleAwareFaceTracker
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.FaceTrackingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FaceSwipeDataModule {

    /**
     * FaceTrackingRepository 인터페이스를 FaceTrackingRepositoryImpl 싱글턴으로 바인딩.
     * domain 레이어에서 인터페이스를 통해 주입받음.
     */
    @Binds
    @Singleton
    abstract fun bindFaceTrackingRepository(
        impl: FaceTrackingRepositoryImpl
    ): FaceTrackingRepository

    /**
     * LifecycleAwareFaceTracker 인터페이스를 같은 싱글턴 인스턴스로 바인딩.
     * FaceSwipeForegroundService에서 bind() 호출용.
     */
    @Binds
    @Singleton
    abstract fun bindLifecycleAwareFaceTracker(
        impl: FaceTrackingRepositoryImpl
    ): LifecycleAwareFaceTracker
}
