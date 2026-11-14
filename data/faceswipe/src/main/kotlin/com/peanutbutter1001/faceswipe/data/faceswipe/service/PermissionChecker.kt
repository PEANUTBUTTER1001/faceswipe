package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 권한 확인 로직을 ViewModel에서 분리.
 *
 * ViewModel이 @ApplicationContext를 직접 사용하지 않도록 하여
 * 테스트 용이성을 높인다.
 */
@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * 알림 표시 가능 여부.
     * API 33+는 POST_NOTIFICATIONS 허용 여부, 그 미만은 사용자가 앱 알림을 끄지 않았는지를 반영한다.
     */
    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** 알림 권한을 런타임에 요청해야 하는 OS 버전인지 (Android 13+) */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun isNotificationPermissionRuntime(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasAccessibilityPermission(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expected = "${context.packageName}/${context.packageName}.data.faceswipe.service.GestureAccessibilityService"

        return enabledServices
            .split(":")
            .any { it.trim().equals(expected, ignoreCase = true) }
    }
}
