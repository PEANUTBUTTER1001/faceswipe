package com.peanutbutter1001.faceswipe.data.faceswipe.di

import com.peanutbutter1001.faceswipe.domain.faceswipe.gesture.GestureDetector
import com.peanutbutter1001.faceswipe.domain.faceswipe.gesture.HeadTurnDetector
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Hilt 멀티바인딩으로 GestureDetector 등록.
 *
 * 새 제스처 추가 시 여기에 @Binds @IntoSet만 추가하면 됨.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GestureModule {

    @Binds
    @IntoSet
    abstract fun bindHeadTurnDetector(impl: HeadTurnDetector): GestureDetector

    // 추후 새 제스처 추가 시:
    // @Binds @IntoSet
    // abstract fun bindHeadNodDetector(impl: HeadNodDetector): GestureDetector

    companion object {
        @Provides
        @Singleton
        fun provideGestureConfig(): GestureConfig = GestureConfig()
        // 추후 DataStore/Room에서 사용자 설정을 읽어 GestureConfig를 동적 생성 가능
    }
}
