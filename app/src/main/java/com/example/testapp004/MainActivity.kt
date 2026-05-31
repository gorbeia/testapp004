package com.example.testapp004

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.testapp004.navigation.AppNavigation
import com.example.testapp004.ui.theme.Testapp004Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Testapp004Theme {
                AppNavigation()
            }
        }
    }
}
