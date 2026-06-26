package com.mundial26.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mundial26.app.ui.theme.MundialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MundialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Placeholder()
                }
            }
        }
    }
}

@Composable
private fun Placeholder() {
    Text(
        text = "Mundial 26",
        modifier = Modifier
            .background(MundialTheme.colors.appBg)
            .wrapContentSize(),
        style = MaterialTheme.typography.titleLarge,
        color = MundialTheme.colors.brand
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderPreview() {
    MundialTheme { Placeholder() }
}
