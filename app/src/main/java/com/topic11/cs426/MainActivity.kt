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
    private lateinit var compositionRoot: FieldFlowCompositionRoot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        compositionRoot = FieldFlowCompositionRoot.create(applicationContext)

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

    override fun onDestroy() {
        compositionRoot.close()
        super.onDestroy()
    }
}
