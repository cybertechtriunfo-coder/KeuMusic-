package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MusicViewModel
import com.example.ui.PulseTubeApp
import com.example.ui.theme.PulseTubeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val musicViewModel: MusicViewModel = viewModel()
            val isDarkTheme by musicViewModel.isDarkTheme.collectAsStateWithLifecycle()

            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            PulseTubeTheme(darkTheme = isDarkTheme) {
                PulseTubeApp(viewModel = musicViewModel)
            }
        }
    }
}
