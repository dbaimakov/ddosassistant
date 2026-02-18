package com.ddosassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ddosassistant.ui.theme.DDoSIncidentAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppServices.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            DDoSIncidentAssistantTheme {
                DdosAssistantApp()
            }
        }
    }
}
