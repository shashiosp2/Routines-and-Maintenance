package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.RoutinaryApp
import com.example.ui.RoutinaryContainer

class MainActivity : ComponentActivity() {
    private val container by lazy { RoutinaryContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoutinaryApp(container = container)
        }
    }
}
