package com.peanutbutter1001.faceswipe.feature.faceswipe.tile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.feature.faceswipe.R

/**
 * Android 12 이하용 빠른 설정 타일 수동 추가 안내.
 * (Android 13+는 TileRequester가 시스템 추가 창을 띄운다)
 */
@Composable
fun TileHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tile_help_title)) },
        text = { Text(stringResource(R.string.tile_help_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) }
        },
    )
}

@Preview(name = "타일 수동 추가 안내")
@Composable
private fun TileHelpDialogPreview() = FaceswipeTheme { TileHelpDialog(onDismiss = {}) }
