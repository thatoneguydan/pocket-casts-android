package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBar
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBarDefaults
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentEffectsBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.PlaybackEffectsSettingsTab
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.updateTint
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.PlaybackContentType
import com.automattic.eventhorizon.PlaybackEffectSettingsViewAppearedEvent
import com.automattic.eventhorizon.PlaybackEffectVolumeBoostToggledEvent
import com.automattic.eventhorizon.SettingType
import com.automattic.eventhorizon.Trackable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class EffectsFragment :
    BaseDialogFragment(),
    CompoundButton.OnCheckedChangeListener {

    override val statusBarIconColor = StatusBarIconColor.Light

    private val viewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentEffectsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
                PlaybackEffectSettingsViewAppearedEvent(
                    source = sourceView.analyticsValue,
                    contentType = contentType,
                    settings = settingType,
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentEffectsBinding.inflate(inflater, container, false)
        binding?.setupEffectsSettingsSegmentedTabBar()

        viewModel.effectsLive.value?.let(::update)
        viewModel.effectsLive.observe(viewLifecycleOwner) { podcastEffectsData ->
            update(podcastEffectsData)
            ensureExpanded()
        }

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun update(podcastEffectsData: PlayerViewModel.PodcastEffectsData) {
        val binding = binding ?: return
        binding.switchVolume.setOnCheckedChangeListener(null)
        binding.switchVolume.isChecked = podcastEffectsData.effects.isVolumeBoosted
        binding.switchVolume.setOnCheckedChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            setDialogTint(backgroundColor)

            val tintColor = theme.playerHighlightColor(viewModel.podcast)
            val playerContrast01 = ThemeColor.playerContrast01(theme.activeTheme)
            binding?.switchVolume?.updateTint(tintColor, playerContrast01)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val binding = binding ?: return
        if (buttonView.id != binding.switchVolume.id) return

        val (podcast, effects) = viewModel.effectsLive.value ?: return
        trackPlaybackEffectsEvent { sourceView, contentType, settingType ->
            PlaybackEffectVolumeBoostToggledEvent(
                enabled = isChecked,
                source = sourceView.analyticsValue,
                contentType = contentType,
                settings = settingType,
            )
        }
        effects.isVolumeBoosted = isChecked
        viewModel.saveEffects(effects, podcast)
    }

    private fun trackPlaybackEffectsEvent(event: (SourceView, PlaybackContentType, SettingType) -> Trackable) {
        viewModel.trackPlaybackEffectsEvent(event)
    }

    private fun FragmentEffectsBinding.setupEffectsSettingsSegmentedTabBar() {
        effectsSettingsSegmentedTabBar.setContent {
            val podcastHeaderBackgroundColor by remember {
                viewModel.playingEpisodeLive.asFlow()
                    .map { it.second }
                    .distinctUntilChangedBy { it }
            }.collectAsStateWithLifecycle(null)

            val podcastEffectsData by remember {
                viewModel.effectsLive.asFlow()
                    .distinctUntilChanged { t1, t2 ->
                        t1.podcast.uuid == t2.podcast.uuid &&
                            t1.podcast.playbackEffects.toData() == t2.podcast.playbackEffects.toData() &&
                            t1.podcast.overrideGlobalEffects == t2.podcast.overrideGlobalEffects &&
                            t1.podcast.overrideGlobalEffectsModified == t2.podcast.overrideGlobalEffectsModified
                    }
            }.collectAsStateWithLifecycle(null)
            val podcast = podcastEffectsData?.podcast ?: return@setContent

            if (podcastEffectsData?.showCustomEffectsSettings == true) {
                val selectedTabTextColor = podcastHeaderBackgroundColor?.let {
                    Color(android.graphics.Color.parseColor(ColorUtils.colorIntToHexString(it)))
                } ?: Color.Black
                EffectsSettingsSegmentedTabBar(
                    selectedItem = if (podcastEffectsData?.podcast?.overrideGlobalEffects == true) {
                        PlaybackEffectsSettingsTab.ThisPodcast
                    } else {
                        PlaybackEffectsSettingsTab.AllPodcasts
                    },
                    selectedTabTextColor = selectedTabTextColor,
                    onItemSelect = {
                        viewModel.onEffectsSettingsSegmentedTabSelected(podcast, PlaybackEffectsSettingsTab.entries[it])
                    },
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }

    @Composable
    private fun EffectsSettingsSegmentedTabBar(
        selectedTabTextColor: Color,
        selectedItem: PlaybackEffectsSettingsTab,
        onItemSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        SegmentedTabBar(
            items = PlaybackEffectsSettingsTab.entries.map { stringResource(it.labelResId) },
            selectedIndex = PlaybackEffectsSettingsTab.entries.indexOf(selectedItem),
            colors = SegmentedTabBarDefaults.colors.copy(
                selectedTabBackgroundColor = MaterialTheme.theme.colors.playerContrast01,
                selectedTabTextColor = selectedTabTextColor,
                borderColor = MaterialTheme.theme.colors.playerContrast03,
            ),
            cornerRadius = 120.dp,
            modifier = modifier.fillMaxWidth(),
            onSelectItem = onItemSelect,
        )
    }

    @Preview(widthDp = 360)
    @Composable
    private fun EffectsSettingsSegmentedBarPreview() {
        EffectsSettingsSegmentedTabBar(
            selectedItem = PlaybackEffectsSettingsTab.AllPodcasts,
            selectedTabTextColor = Color.Black,
            onItemSelect = {},
        )
    }
}
