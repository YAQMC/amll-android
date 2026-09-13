package dev.yaqmc.amll.consumer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.yaqmc.amll.state.AMLLPlayerState
import dev.yaqmc.amll.ui.AMLLPlayer
import dev.yaqmc.amll.ui.AMLLStyle

/** Compiles only through the published Maven artifact; no project dependency is available here. */
@Composable
fun PublishedArtifactSmoke(
    state: AMLLPlayerState = AMLLPlayerState(),
    modifier: Modifier = Modifier,
) {
    AMLLPlayer(
        state = state,
        modifier = modifier,
        style = AMLLStyle(),
    )
}
