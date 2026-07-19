package com.topic11.cs426

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.topic11.cs426.core.designsystem.FieldFlowTheme
import com.topic11.cs426.core.navigation.DashboardScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val compositionRoot = FieldFlowCompositionRoot.create()

        setContent {
            FieldFlowTheme {
                val navStack = rememberSaveableNavStack(root = DashboardScreen)
                val navigator = rememberCircuitNavigator(navStack) {
                    finish()
                }

                CircuitCompositionLocals(compositionRoot.circuit) {
                    NavigableCircuitContent(
                        navigator = navigator,
                        navStack = navStack,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
