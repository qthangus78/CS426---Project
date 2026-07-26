package com.topic11.cs426

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
    private val compositionRoot: FieldFlowCompositionRoot
        get() = (application as FieldFlowApplication).compositionRoot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                // enableBackHandler has to be passed explicitly: dropping it selects the overload
                // that registers no back handler at all — Kotlin prefers the candidate that needs no
                // default argument — and system back then falls through to the Activity and finishes
                // it from every screen. Circuit disables its own handler at the root, so back on the
                // dashboard still exits the app through onRootPop.
                val navigator = rememberCircuitNavigator(
                    navStack = navStack,
                    onRootPop = { finish() },
                    enableBackHandler = true,
                )
                val evidenceCaptureHandler = rememberEvidenceCaptureHandler(
                    evidenceStore = compositionRoot.evidenceStore,
                )
                val seedingState by compositionRoot.sampleDataSeedingState.collectAsState()

                CircuitCompositionLocals(compositionRoot.circuit) {
                    CompositionLocalProvider(
                        LocalInspectionEvidenceCaptureHandler provides evidenceCaptureHandler,
                    ) {
                        // The banner overlays the content instead of sitting above it: seeding fails
                        // rarely, and an overlay keeps every screen's layout identical in the normal
                        // case where nothing is shown at all.
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavigableCircuitContent(
                                navigator = navigator,
                                navStack = navStack,
                                modifier = Modifier.fillMaxSize(),
                            )
                            SampleDataSeedingFailureBanner(
                                state = seedingState,
                                onRetry = compositionRoot::retrySampleDataSeeding,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding(),
                            )
                        }
                    }
                }
            }
        }
    }
}
