package com.wiantex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wiantex.app.ui.WiantexApp
import com.wiantex.app.ui.WiantexViewModel
import com.wiantex.app.ui.theme.WiantexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiantexTheme {
                val vm: WiantexViewModel = viewModel()
                WiantexApp(vm)
            }
        }
    }
}
