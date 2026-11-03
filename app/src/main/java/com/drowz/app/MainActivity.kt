package com.drowz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.drowz.app.ui.DrowzApp
import com.drowz.app.ui.theme.DrowzTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as DrowzApplication).container
        setContent {
            DrowzTheme {
                DrowzApp(container)
            }
        }
    }
}
