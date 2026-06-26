package com.mundial26.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mundial26.app.ui.theme.MundialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MundialApplication).container
        setContent {
            MundialTheme {
                MundialApp(container)
            }
        }
    }
}
