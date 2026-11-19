package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.ui.screens.TasksHomeRoute
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.DaylistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaylistTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var hasEntered by rememberSaveable { mutableStateOf(false) }

    Crossfade(targetState = hasEntered, label = "app_entry") { entered ->
        if (!entered) {
            WelcomeScreen(onEnterClick = { hasEntered = true })
        } else {
            TasksHomeRoute()
        }
    }
}
