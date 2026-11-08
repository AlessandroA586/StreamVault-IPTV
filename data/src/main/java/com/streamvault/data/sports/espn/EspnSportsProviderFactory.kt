package com.streamvault.data.sports.espn

import com.streamvault.data.sports.provider.SportsProviderHttpClient
import com.streamvault.domain.sports.provider.*
import java.time.LocalDate
import javax.inject.Inject

class EspnSportsProviderFactory @Inject constructor(
    private val httpClient: SportsProviderHttpClient,
    private val cache: EspnResponseCache,
) : SportsProviderFactory {

    override val descriptor = SportsProviderDescriptor(
        typeId = TYPE_ID,
        capabilities = setOf(
            SportsCapability.EVENTS,
            SportsCapability.LIVE_SCORES,
            SportsCapability.GAME_STATUS,
            SportsCapability.STANDINGS,
            SportsCapability.ARTWORK,
            SportsCapability.TEAM_METADATA,
            SportsCapability.BROADCASTERS,
        ),
        auth = SportsProviderAuth.None,
        defaultBaseUrl = BASE_URL,
        displayName = "ESPN (Nuvio Feed)",
        compliance = ProviderComplianceInfo(
            termsUrl = "https://www.espn.com/espn/termsOfUse",
            documentationUrl = "https://site.api.espn.com",
            reviewedAt = LocalDate.of(2026, 11, 8),
            attribution = ProviderAttribution(
                sourceName = "ESPN",
                sourceUrl = "https://www.espn.com",
            ),
            appStoreAllowed = true,
            commercialUseStatus = CommercialUseStatus.PERMITTED,
            artworkStatus = ArtworkUseStatus.CLEARED,
            releaseGate = ProviderReleaseGate.APPROVED,
        ),
    )

    override fun capabilitiesFor(config: SportsProviderConfig): Set<SportsCapability> =
        descriptor.capabilities

    override fun create(config: SportsProviderConfig): SportsProvider = EspnSportsProvider(
        config = config,
        descriptor = descriptor,
        httpClient = httpClient,
        cache = cache,
    )

    companion object {
        const val TYPE_ID = "espn"
        const val BASE_URL = "https://site.api.espn.com/apis/site/v2/sports"
        const val NUVIO_DATA_MIRROR = "https://raw.githubusercontent.com/homelessbrian/nuvio-data/main"

        val ALL_SCOPES: List<EspnLeagueScope> = listOf(
            // Football / Soccer
            EspnLeagueScope("soccer", "eng.1", "Premier League", "Football"),
            EspnLeagueScope("soccer", "esp.1", "LaLiga", "Football"),
            EspnLeagueScope("soccer", "ita.1", "Serie A", "Football"),
            EspnLeagueScope("soccer", "ger.1", "Bundesliga", "Football"),
            EspnLeagueScope("soccer", "fra.1", "Ligue 1", "Football"),
            EspnLeagueScope("soccer", "uefa.champions", "Champions League", "Football"),
            EspnLeagueScope("soccer", "uefa.europa", "Europa League", "Football"),
            EspnLeagueScope("soccer", "usa.1", "MLS", "Football"),
            EspnLeagueScope("soccer", "mex.1", "Liga MX", "Football"),
            EspnLeagueScope("soccer", "conmebol.libertadores", "Copa Libertadores", "Football"),
            // Basketball
            EspnLeagueScope("basketball", "nba", "NBA", "Basketball"),
            EspnLeagueScope("basketball", "wnba", "WNBA", "Basketball"),
            EspnLeagueScope("basketball", "mens-college-basketball", "College Basketball", "Basketball"),
            // American Football
            EspnLeagueScope("football", "nfl", "NFL", "American football"),
            EspnLeagueScope("football", "college-football", "College Football", "American football"),
            // Baseball & Hockey
            EspnLeagueScope("baseball", "mlb", "MLB", "Baseball"),
            EspnLeagueScope("hockey", "nhl", "NHL", "Hockey"),
            // Motorsport
            EspnLeagueScope("racing", "f1", "Formula 1", "Motorsport"),
            EspnLeagueScope("racing", "nascar-cup", "NASCAR Cup", "Motorsport"),
            EspnLeagueScope("racing", "indycar", "IndyCar", "Motorsport"),
            // Tennis
            EspnLeagueScope("tennis", "atp", "ATP Tennis", "Tennis"),
            EspnLeagueScope("tennis", "wta", "WTA Tennis", "Tennis"),
            // MMA / Combat
            EspnLeagueScope("mma", "ufc", "UFC / MMA", "MMA"),
            EspnLeagueScope("boxing", "boxing", "Boxing", "Boxing"),
            // Golf
            EspnLeagueScope("golf", "pga", "PGA Tour", "Golf"),
            EspnLeagueScope("golf", "lpga", "LPGA Tour", "Golf"),
        )

        val DEFAULT_SCOPES: List<EspnLeagueScope> = ALL_SCOPES
    }
}
