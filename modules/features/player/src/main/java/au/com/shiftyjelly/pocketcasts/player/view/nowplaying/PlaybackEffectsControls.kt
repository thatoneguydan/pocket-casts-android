package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.utils.Debouncer
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import com.automattic.eventhorizon.PlaybackEffectSpeedChangedEvent
import com.automattic.eventhorizon.PlaybackEffectTrimSilenceAmountChangedEvent
import com.automattic.eventhorizon.PlaybackEffectTrimSilenceToggledEvent
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlaybackEffectsControls(
    playerColors: PlayerColors,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val effectsData by playerViewModel.effectsLive.observeAsState()
    val currentData = effectsData ?: return
    val stateKey = "${currentData.podcast.uuid}:${currentData.podcast.overrideGlobalEffects}"

    var playbackSpeed by remember(stateKey) {
        mutableStateOf(currentData.effects.playbackSpeed.roundedSpeed())
    }
    var trimMode by remember(stateKey) {
        mutableStateOf(currentData.effects.trimMode)
    }

    LaunchedEffect(
        stateKey,
        currentData.effects.playbackSpeed,
        currentData.effects.trimMode,
    ) {
        playbackSpeed = currentData.effects.playbackSpeed.roundedSpeed()
        trimMode = currentData.effects.trimMode
    }

    val coroutineScope = rememberCoroutineScope()
    val playbackSpeedTrackingDebouncer = remember { Debouncer() }

    fun changePlaybackSpeed(amount: Double) {
        val latestData = playerViewModel.effectsLive.value ?: return
        val speed = amount.roundedSpeed()
        latestData.effects.playbackSpeed = speed
        playbackSpeed = speed
        playerViewModel.saveEffects(latestData.effects, latestData.podcast)

        coroutineScope.launch {
            playbackSpeedTrackingDebouncer.debounce {
                playerViewModel.effectsLive.value?.effects?.playbackSpeed?.roundedSpeed()?.let { currentSpeed ->
                    playerViewModel.trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
                        PlaybackEffectSpeedChangedEvent(
                            speed = currentSpeed,
                            source = sourceView.analyticsValue,
                            contentType = contentType,
                            settings = settingType,
                        )
                    }
                }
            }
        }
    }

    fun setTrimMode(newTrimMode: TrimMode) {
        val latestData = playerViewModel.effectsLive.value ?: return
        val previousTrimMode = latestData.effects.trimMode
        if (previousTrimMode == newTrimMode) return

        latestData.effects.trimMode = newTrimMode
        trimMode = newTrimMode

        val wasEnabled = previousTrimMode != TrimMode.OFF
        val isEnabled = newTrimMode != TrimMode.OFF
        if (wasEnabled != isEnabled) {
            playerViewModel.trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
                PlaybackEffectTrimSilenceToggledEvent(
                    enabled = isEnabled,
                    source = sourceView.analyticsValue,
                    contentType = contentType,
                    settings = settingType,
                )
            }
        }
        if (isEnabled) {
            playerViewModel.trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
                PlaybackEffectTrimSilenceAmountChangedEvent(
                    amount = newTrimMode.analyticsValue,
                    source = sourceView.analyticsValue,
                    contentType = contentType,
                    settings = settingType,
                )
            }
        }
        playerViewModel.saveEffects(latestData.effects, latestData.podcast)
    }

    Surface(
        color = playerColors.background02,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, playerColors.contrast05),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .padding(start = 12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_speed),
                    contentDescription = null,
                    tint = playerColors.contrast02,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(LR.string.player_effects_speed),
                    color = playerColors.contrast01,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { changePlaybackSpeed(playbackSpeed - 0.1) },
                    modifier = Modifier
                        .width(56.dp)
                        .height(48.dp),
                ) {
                    Text(
                        text = "−",
                        color = playerColors.contrast02,
                        style = MaterialTheme.typography.h5,
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(44.dp)
                        .widthIn(min = 84.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, playerColors.contrast04, RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) {
                            val nextSpeed = when (playbackSpeed.roundedSpeed()) {
                                1.0 -> 1.5
                                1.5 -> 2.0
                                else -> 1.0
                            }
                            changePlaybackSpeed(nextSpeed)
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = String.format("%.1fx", playbackSpeed),
                        color = playerColors.contrast01,
                        style = MaterialTheme.typography.subtitle1,
                    )
                }
                IconButton(
                    onClick = { changePlaybackSpeed(playbackSpeed + 0.1) },
                    modifier = Modifier
                        .width(56.dp)
                        .height(48.dp),
                ) {
                    Text(
                        text = "+",
                        color = playerColors.contrast02,
                        style = MaterialTheme.typography.h5,
                    )
                }
            }

            Divider(color = playerColors.contrast05.copy(alpha = 0.7f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                TrimModeButton(
                    text = stringResource(LR.string.off),
                    selected = trimMode == TrimMode.OFF,
                    position = TrimButtonPosition.Left,
                    playerColors = playerColors,
                    onClick = { setTrimMode(TrimMode.OFF) },
                    modifier = Modifier.weight(1f),
                )
                TrimModeDivider(playerColors)
                TrimModeButton(
                    text = stringResource(LR.string.player_effects_trim_mild),
                    selected = trimMode == TrimMode.LOW,
                    position = TrimButtonPosition.Middle,
                    playerColors = playerColors,
                    onClick = { setTrimMode(TrimMode.LOW) },
                    modifier = Modifier.weight(1f),
                )
                TrimModeDivider(playerColors)
                TrimModeButton(
                    text = stringResource(LR.string.player_effects_trim_medium),
                    selected = trimMode == TrimMode.MEDIUM,
                    position = TrimButtonPosition.Middle,
                    playerColors = playerColors,
                    onClick = { setTrimMode(TrimMode.MEDIUM) },
                    modifier = Modifier.weight(1f),
                )
                TrimModeDivider(playerColors)
                TrimModeButton(
                    text = stringResource(LR.string.player_effects_trim_mad_max),
                    selected = trimMode == TrimMode.HIGH,
                    position = TrimButtonPosition.Right,
                    playerColors = playerColors,
                    onClick = { setTrimMode(TrimMode.HIGH) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TrimModeButton(
    text: String,
    selected: Boolean,
    position: TrimButtonPosition,
    playerColors: PlayerColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = when (position) {
        TrimButtonPosition.Left -> RoundedCornerShape(bottomStart = 13.dp)
        TrimButtonPosition.Middle -> RoundedCornerShape(0.dp)
        TrimButtonPosition.Right -> RoundedCornerShape(bottomEnd = 13.dp)
    }
    val backgroundColor = if (selected) playerColors.contrast01 else Color.Transparent
    val textColor = if (selected) playerColors.background01 else playerColors.contrast01

    Surface(
        color = backgroundColor,
        shape = shape,
        modifier = modifier
            .height(48.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.button,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TrimModeDivider(playerColors: PlayerColors) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .background(playerColors.contrast04),
    )
}

private enum class TrimButtonPosition {
    Left,
    Middle,
    Right,
}
