package com.topic11.cs426

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.topic11.cs426.core.designsystem.FieldFlowTheme
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.domain.model.ThemeMode
import com.topic11.cs426.feature.inspection.LocalInspectionEvidenceCaptureHandler

class MainActivity : ComponentActivity() {
    private lateinit var compositionRoot: FieldFlowCompositionRoot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        compositionRoot = FieldFlowCompositionRoot.create(applicationContext)

        setContent {
            val selectedThemeMode by compositionRoot.observeThemeMode()
                .collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (selectedThemeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FieldFlowTheme(darkTheme = darkTheme) {
                val navStack = rememberSaveableNavStack(root = DashboardScreen)
                val navigator = rememberCircuitNavigator(navStack) {
                    finish()
                }
                val evidenceCaptureHandler = rememberEvidenceCaptureHandler(
                    evidenceStore = compositionRoot.evidenceStore,
                )

                CircuitCompositionLocals(compositionRoot.circuit) {
                    CompositionLocalProvider(
                        LocalInspectionEvidenceCaptureHandler provides evidenceCaptureHandler,
                    ) {
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

    override fun onDestroy() {
        compositionRoot.close()
        super.onDestroy()
    }
}
