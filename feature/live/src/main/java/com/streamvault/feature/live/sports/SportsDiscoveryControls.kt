package com.streamvault.feature.live.sports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamvault.core.ui.components.dialogs.AppDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.Icon
import androidx.tv.material3.ButtonDefaults
import com.streamvault.core.ui.design.BrowseColors
import com.streamvault.core.ui.design.BrowseShapes
import com.streamvault.core.ui.interaction.TvButton
import com.streamvault.core.ui.interaction.TvClickableSurface
import com.streamvault.feature.live.R

@Composable
internal fun SportsDiscoverySearch(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var acceptsInput by remember { mutableStateOf(false) }
    var inputFocused by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchLabel = stringResource(R.string.live_sports_search_events)

    LaunchedEffect(acceptsInput) {
        if (acceptsInput) {
            withFrameNanos { }
            inputFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(modifier = modifier.widthIn(min = 200.dp, max = 320.dp).height(36.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = {
                Text(
                    text = searchLabel,
                    fontSize = 11.sp,
                    color = BrowseColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = BrowseColors.TextSecondary,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TvButton(
                        onClick = { onQuery("") },
                        modifier = Modifier.size(24.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = BrowseColors.TextSecondary,
                            focusedContainerColor = BrowseColors.SurfaceElevated,
                            focusedContentColor = BrowseColors.TextPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.epg_clear_search),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(inputFocusRequester)
                .focusProperties { canFocus = acceptsInput }
                .onFocusChanged { focusState ->
                    val lostFocus = inputFocused && !focusState.isFocused
                    inputFocused = focusState.isFocused
                    if (lostFocus) {
                        acceptsInput = false
                        keyboardController?.hide()
                    }
                },
            readOnly = !acceptsInput,
            shape = BrowseShapes.Control,
            colors = sportsSearchColors(),
        )
        if (!acceptsInput) {
            TvClickableSurface(
                onClick = { acceptsInput = true },
                modifier = Modifier
                    .matchParentSize()
                    .semantics { contentDescription = searchLabel },
                shape = ClickableSurfaceDefaults.shape(BrowseShapes.Control),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    contentColor = Color.Transparent,
                    focusedContentColor = Color.Transparent,
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border(BorderStroke(0.dp, Color.Transparent), shape = BrowseShapes.Control),
                    focusedBorder = Border(BorderStroke(2.dp, BrowseColors.Focus), shape = BrowseShapes.Control),
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            ) { }
        }
    }
}

@Composable
internal fun SportsInclusionsDialog(
    state: SportsHubUiState,
    onSport: (SportType, Boolean) -> Unit,
    onLeague: (String, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var leagueQuery by remember { mutableStateOf("") }
    val localizedLeagueNames = sportsLeagueLabels()
    val leagues = (state.excludedLeagues.associateWith { it } + state.knownLeagues)
        .mapValues { (id, name) -> localizedLeagueNames[id] ?: name }.entries
        .filter { SportsTextNormalizer.normalize(it.value).contains(SportsTextNormalizer.normalize(leagueQuery)) }
        .sortedBy { it.value }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 880.dp).fillMaxHeight(0.92f),
            colors = SurfaceDefaults.colors(containerColor = BrowseColors.SurfaceElevated),
            shape = BrowseShapes.Panel,
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.live_sports_inclusions), fontSize = 22.sp, color = BrowseColors.TextPrimary)
                Text(stringResource(R.string.live_sports_inclusions_hint), fontSize = 12.sp, color = BrowseColors.TextSecondary)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.nav_sports), color = BrowseColors.TextPrimary)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(4.dp)) {
                            items(SportType.entries.filter { it != SportType.UNKNOWN }, key = { it.name }) { sport ->
                                InclusionRow(stringResource(sport.labelResource()), sport.discoveryKey !in state.excludedSports) {
                                    onSport(sport, sport.discoveryKey in state.excludedSports)
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.live_sports_leagues), color = BrowseColors.TextPrimary)
                        OutlinedTextField(
                            value = leagueQuery, onValueChange = { leagueQuery = it }, singleLine = true,
                            label = { Text(stringResource(R.string.live_sports_search_leagues), color = BrowseColors.TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = sportsSearchColors(),
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(4.dp)) {
                            items(leagues, key = { it.key }) { league ->
                                InclusionRow(league.value, league.key !in state.excludedLeagues) {
                                    onLeague(league.key, league.key in state.excludedLeagues)
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton(onClick = onReset) { Text(stringResource(R.string.hidden_categories_dialog_unhide_all)) }
                    Spacer(Modifier.weight(1f))
                    TvButton(onClick = onDismiss) { Text(stringResource(R.string.hidden_categories_dialog_close)) }
                }
            }
        }
    }
}

@Composable
private fun sportsSearchColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = BrowseColors.TextPrimary,
    unfocusedTextColor = BrowseColors.TextPrimary,
    cursorColor = BrowseColors.Brand,
    focusedBorderColor = BrowseColors.Brand,
    unfocusedBorderColor = BrowseColors.TextSecondary,
)

@Composable
private fun InclusionRow(label: String, included: Boolean, onClick: () -> Unit) {
    TvClickableSurface(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        toggleableState = if (included) ToggleableState.On else ToggleableState.Off
    }) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(included, onCheckedChange = null, colors = CheckboxDefaults.colors(
                checkedColor = BrowseColors.Brand, uncheckedColor = BrowseColors.TextSecondary,
            ))
            Text(label, color = BrowseColors.TextPrimary, modifier = Modifier.padding(start = 6.dp), maxLines = 2)
        }
    }
}

internal fun SportType.icon(): ImageVector = when (this) {
    SportType.FOOTBALL -> Icons.Filled.SportsSoccer
    SportType.BASKETBALL -> Icons.Filled.SportsBasketball
    SportType.MOTORSPORT -> Icons.Filled.SportsMotorsports
    SportType.AMERICAN_FOOTBALL -> Icons.Filled.SportsFootball
    SportType.BASEBALL -> Icons.Filled.SportsBaseball
    SportType.HOCKEY -> Icons.Filled.SportsHockey
    SportType.TENNIS -> Icons.Filled.SportsTennis
    SportType.MMA, SportType.BOXING -> Icons.Filled.SportsMma
    SportType.CRICKET -> Icons.Filled.SportsCricket
    SportType.RUGBY -> Icons.Filled.SportsRugby
    SportType.GOLF -> Icons.Filled.SportsGolf
    SportType.VOLLEYBALL -> Icons.Filled.SportsVolleyball
    SportType.HANDBALL -> Icons.Filled.SportsHandball
    SportType.CYCLING -> Icons.Filled.DirectionsBike
    SportType.SNOOKER, SportType.DARTS, SportType.OTHER, SportType.UNKNOWN -> Icons.Filled.Sports
}

internal fun SportType.labelResource(): Int = when (this) {
    SportType.FOOTBALL -> R.string.live_sports_filter_football
    SportType.BASKETBALL -> R.string.live_sports_filter_basketball
    SportType.MOTORSPORT -> R.string.live_sports_filter_motorsport
    SportType.AMERICAN_FOOTBALL -> R.string.live_sports_american_football
    SportType.BASEBALL -> R.string.live_sports_baseball
    SportType.HOCKEY -> R.string.live_sports_hockey
    SportType.TENNIS -> R.string.live_sports_tennis
    SportType.MMA -> R.string.live_sports_mma
    SportType.BOXING -> R.string.live_sports_boxing
    SportType.CRICKET -> R.string.live_sports_cricket
    SportType.RUGBY -> R.string.live_sports_rugby
    SportType.GOLF -> R.string.live_sports_golf
    SportType.VOLLEYBALL -> R.string.live_sports_volleyball
    SportType.HANDBALL -> R.string.live_sports_handball
    SportType.SNOOKER -> R.string.live_sports_snooker
    SportType.DARTS -> R.string.live_sports_darts
    SportType.CYCLING -> R.string.live_sports_cycling
    SportType.OTHER, SportType.UNKNOWN -> R.string.live_sports_filter_other
}

internal fun SportsSportFilter.icon(): ImageVector = when (this) {
    SportsSportFilter.ALL -> Icons.Filled.GridView
    SportsSportFilter.FOOTBALL -> Icons.Filled.SportsSoccer
    SportsSportFilter.BASKETBALL -> Icons.Filled.SportsBasketball
    SportsSportFilter.MOTORSPORT -> Icons.Filled.SportsMotorsports
    SportsSportFilter.TENNIS -> Icons.Filled.SportsTennis
    SportsSportFilter.BOXING -> Icons.Filled.SportsMma
    SportsSportFilter.CRICKET -> Icons.Filled.SportsCricket
    SportsSportFilter.CYCLING -> Icons.Filled.DirectionsBike
    SportsSportFilter.MMA -> Icons.Filled.SportsMma
    SportsSportFilter.AMERICAN_FOOTBALL -> Icons.Filled.SportsFootball
    SportsSportFilter.BASEBALL -> Icons.Filled.SportsBaseball
    SportsSportFilter.HOCKEY -> Icons.Filled.SportsHockey
    SportsSportFilter.RUGBY -> Icons.Filled.SportsRugby
    SportsSportFilter.GOLF -> Icons.Filled.SportsGolf
    SportsSportFilter.OTHER -> Icons.Filled.Sports
}

internal fun SportsSportFilter.labelResource(): Int = when (this) {
    SportsSportFilter.ALL -> R.string.home_quick_filters_all
    SportsSportFilter.FOOTBALL -> R.string.live_sports_filter_football
    SportsSportFilter.BASKETBALL -> R.string.live_sports_filter_basketball
    SportsSportFilter.MOTORSPORT -> R.string.live_sports_filter_motorsport
    SportsSportFilter.TENNIS -> R.string.live_sports_tennis
    SportsSportFilter.BOXING -> R.string.live_sports_boxing
    SportsSportFilter.CRICKET -> R.string.live_sports_cricket
    SportsSportFilter.CYCLING -> R.string.live_sports_cycling
    SportsSportFilter.MMA -> R.string.live_sports_mma
    SportsSportFilter.AMERICAN_FOOTBALL -> R.string.live_sports_american_football
    SportsSportFilter.BASEBALL -> R.string.live_sports_baseball
    SportsSportFilter.HOCKEY -> R.string.live_sports_hockey
    SportsSportFilter.RUGBY -> R.string.live_sports_rugby
    SportsSportFilter.GOLF -> R.string.live_sports_golf
    SportsSportFilter.OTHER -> R.string.live_sports_filter_other
}

internal val SportsSportFilter.discoveryKey: String?
    get() = when (this) {
        SportsSportFilter.ALL -> null
        SportsSportFilter.FOOTBALL -> SportType.FOOTBALL.discoveryKey
        SportsSportFilter.BASKETBALL -> SportType.BASKETBALL.discoveryKey
        SportsSportFilter.MOTORSPORT -> SportType.MOTORSPORT.discoveryKey
        SportsSportFilter.TENNIS -> SportType.TENNIS.discoveryKey
        SportsSportFilter.BOXING -> SportType.BOXING.discoveryKey
        SportsSportFilter.CRICKET -> SportType.CRICKET.discoveryKey
        SportsSportFilter.CYCLING -> SportType.CYCLING.discoveryKey
        SportsSportFilter.MMA -> SportType.MMA.discoveryKey
        SportsSportFilter.AMERICAN_FOOTBALL -> SportType.AMERICAN_FOOTBALL.discoveryKey
        SportsSportFilter.BASEBALL -> SportType.BASEBALL.discoveryKey
        SportsSportFilter.HOCKEY -> SportType.HOCKEY.discoveryKey
        SportsSportFilter.RUGBY -> SportType.RUGBY.discoveryKey
        SportsSportFilter.GOLF -> SportType.GOLF.discoveryKey
        SportsSportFilter.OTHER -> SportType.OTHER.discoveryKey
    }
