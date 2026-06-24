package com.peanutbutter1001.faceswipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.navigation.FaceSwipeNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceswipeTheme {
                FaceSwipeNavHost()
            }
        }
    }
}
