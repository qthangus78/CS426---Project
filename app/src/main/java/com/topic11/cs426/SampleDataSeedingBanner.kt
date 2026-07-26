package com.topic11.cs426

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Reports a failed startup seed to the user. Renders nothing while seeding runs or once it
 * succeeded — the only state worth interrupting for is the one that leaves every screen empty.
 *
 * Lives in `:app` rather than in a feature module so surfacing the failure does not push app-scoped
 * startup state into `:feature:dashboard`.
 */
@Composable
internal fun SampleDataSeedingFailureBanner(
    state: SampleDataSeedingState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val failure = state as? SampleDataSeedingState.Failed ?: return

    Snackbar(
        modifier = modifier
            .padding(16.dp)
            .testTag(SAMPLE_DATA_SEEDING_FAILURE_TAG),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        action = {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        },
    ) {
        Text(seedingFailureMessage(failure.cause))
    }
}

internal const val SAMPLE_DATA_SEEDING_FAILURE_TAG = "sample-data-seeding-failure"

private const val BASE_MESSAGE =
    "Couldn't load sample data, so screens may look empty."

/**
 * Keeps the cause visible: a seeding failure is almost always a Room or storage problem, and the
 * exception message is the only hint a user can pass on when reporting it.
 */
private fun seedingFailureMessage(cause: Throwable): String {
    val detail = cause.message?.trim()?.takeIf { it.isNotEmpty() } ?: return BASE_MESSAGE

    return "$BASE_MESSAGE ($detail)"
}
