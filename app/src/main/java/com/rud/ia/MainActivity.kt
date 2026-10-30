package com.rud.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RudTheme {
                var splashFinished by remember { mutableStateOf(false) }

                Crossfade(targetState = splashFinished, label = "splash") { finished ->
                    if (finished) {
                        ChatScreen()
                    } else {
                        SplashScreen(onFinished = { splashFinished = true })
                    }
                }
            }
        }
    }
}
