package com.peanutbutter1001.faceswipe.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * 고정 브랜드 팔레트 (명세 Q3 = A). outputs/faceswipe-prototype.html의 토큰과 같은 값.
 * 기기 배경화면에 따라 색이 바뀌는 동적 색상 대신, 안내 화면·스토어 스크린샷과 같은 색을 보장한다.
 * 지정하지 않은 역할(surfaceContainer 등)은 Material 기본값을 사용한다.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2F5FD0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5FB),
    onPrimaryContainer = Color(0xFF0A2A6B),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1F),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFF2F3F7),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF62666E),
    outlineVariant = Color(0xFFE1E3E8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9C2FF),
    onPrimary = Color(0xFF0A2A6B),
    primaryContainer = Color(0xFF23345E),
    onPrimaryContainer = Color(0xFFDCE5FB),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E5EA),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E5EA),
    surfaceVariant = Color(0xFF1B1E24),
    onSurfaceVariant = Color(0xFFC1C5CD),
    outline = Color(0xFF9DA2AB),
    outlineVariant = Color(0xFF2C3038),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1712),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Material ColorScheme에 없는 의미 색상 토큰.
 *
 * android-settings.md 4절: 배경/텍스트/브랜드/위험 외에 "성공" 역할을 별도 토큰으로 둔다.
 * 위험(error)은 MaterialTheme.colorScheme.error를 그대로 사용한다.
 */
@Immutable
data class FaceswipeStatusColors(
    val success: Color,
)

private val LightStatusColors = FaceswipeStatusColors(success = Color(0xFF1B7F3B))
private val DarkStatusColors = FaceswipeStatusColors(success = Color(0xFF7DD99A))

val LocalFaceswipeStatusColors = staticCompositionLocalOf { LightStatusColors }

/** 현재 테마의 의미 색상 토큰 접근자 */
object FaceswipeThemeTokens {
    val statusColors: FaceswipeStatusColors
        @Composable get() = LocalFaceswipeStatusColors.current
}

@Composable
fun FaceswipeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 고정 팔레트 사용 (명세 Q3 = A). true로 넘기면 Android 12+ 동적 색상
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalFaceswipeStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
