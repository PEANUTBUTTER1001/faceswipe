package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
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
