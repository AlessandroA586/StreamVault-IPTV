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
        const val TYPE_ID = ESPN_SPORTS_PROVIDER_TYPE_ID
        const val BASE_URL = "https://site.api.espn.com/apis/site/v2/sports"
        const val NUVIO_DATA_MIRROR = "https://raw.githubusercontent.com/homelessbrian/nuvio-data/main"
        const val WRESTLING_ENDPOINT = "https://raw.githubusercontent.com/homelessbrian/nuvio-data/main/wrestling.json"
        const val CRICKET_PANEL_ENDPOINT = "https://site.web.api.espn.com/apis/site/v2/sports/cricket/scorepanel"
        const val RUGBY_PANEL_ENDPOINT = "https://site.api.espn.com/apis/site/v2/sports/rugby/scorepanel"

        val ALL_SCOPES: List<EspnLeagueScope> = listOf(
            // Football / Soccer - Major European Leagues
            EspnLeagueScope("soccer", "eng.1", "Premier League", "Football"),
            EspnLeagueScope("soccer", "esp.1", "LaLiga", "Football"),
            EspnLeagueScope("soccer", "ita.1", "Serie A", "Football"),
            EspnLeagueScope("soccer", "ger.1", "Bundesliga", "Football"),
            EspnLeagueScope("soccer", "fra.1", "Ligue 1", "Football"),
            EspnLeagueScope("soccer", "ned.1", "Eredivisie", "Football"),
            EspnLeagueScope("soccer", "por.1", "Primeira Liga", "Football"),
            EspnLeagueScope("soccer", "sco.1", "Scottish Premiership", "Football"),
            EspnLeagueScope("soccer", "tur.1", "Süper Lig", "Football"),
            EspnLeagueScope("soccer", "bel.1", "Belgian Pro League", "Football"),

            // Soccer - Continental & Tournaments
            EspnLeagueScope("soccer", "uefa.champions", "Champions League", "Football"),
            EspnLeagueScope("soccer", "uefa.europa", "Europa League", "Football"),
            EspnLeagueScope("soccer", "uefa.europa.conf", "Conference League", "Football"),
            EspnLeagueScope("soccer", "uefa.nations", "UEFA Nations League", "Football"),
            EspnLeagueScope("soccer", "conmebol.libertadores", "Copa Libertadores", "Football"),
            EspnLeagueScope("soccer", "conmebol.sudamericana", "Copa Sudamericana", "Football"),
            EspnLeagueScope("soccer", "concacaf.champions", "Concacaf Champions Cup", "Football"),
            EspnLeagueScope("soccer", "afc.champions", "AFC Champions League", "Football"),

            // Soccer - Americas & Worldwide
            EspnLeagueScope("soccer", "usa.1", "MLS", "Football"),
            EspnLeagueScope("soccer", "mex.1", "Liga MX", "Football"),
            EspnLeagueScope("soccer", "arg.1", "Primera División Argentina", "Football"),
            EspnLeagueScope("soccer", "bra.1", "Brasileirão Série A", "Football"),
            EspnLeagueScope("soccer", "col.1", "Liga BetPlay Colombia", "Football"),
            EspnLeagueScope("soccer", "ksa.1", "Saudi Pro League", "Football"),

            // Basketball
            EspnLeagueScope("basketball", "nba", "NBA", "Basketball"),
            EspnLeagueScope("basketball", "wnba", "WNBA", "Basketball"),
            EspnLeagueScope("basketball", "mens-college-basketball", "College Basketball", "Basketball"),
            EspnLeagueScope("basketball", "nbl", "NBL Australia", "Basketball"),

            // American Football
            EspnLeagueScope("football", "nfl", "NFL", "American football"),
            EspnLeagueScope("football", "college-football", "College Football", "American football"),
            EspnLeagueScope("football", "cfl", "CFL", "American football"),
            EspnLeagueScope("football", "ufl", "UFL", "American football"),

            // Baseball
            EspnLeagueScope("baseball", "mlb", "MLB", "Baseball"),
            EspnLeagueScope("baseball", "world-baseball-classic", "World Baseball Classic", "Baseball"),
            EspnLeagueScope("baseball", "caribbean-series", "Serie del Caribe", "Baseball"),
            EspnLeagueScope("baseball", "dominican-winter-league", "LIDOM Dominicana", "Baseball"),
            EspnLeagueScope("baseball", "mexican-winter-league", "Liga Mexicana del Pacífico", "Baseball"),
            EspnLeagueScope("baseball", "venezuelan-winter-league", "LVBP Venezuela", "Baseball"),

            // Ice Hockey
            EspnLeagueScope("hockey", "nhl", "NHL", "Hockey"),
            EspnLeagueScope("hockey", "mens-college-hockey", "College Hockey", "Hockey"),

            // Motorsport / Racing
            EspnLeagueScope("racing", "f1", "Formula 1", "Motorsport"),
            EspnLeagueScope("racing", "nascar-premier", "NASCAR Cup", "Motorsport"),
            EspnLeagueScope("racing", "nascar-secondary", "NASCAR Xfinity", "Motorsport"),
            EspnLeagueScope("racing", "nascar-truck", "NASCAR Craftsman Truck", "Motorsport"),
            EspnLeagueScope("racing", "irl", "IndyCar", "Motorsport"),

            // Tennis
            EspnLeagueScope("tennis", "atp", "ATP Tennis", "Tennis"),
            EspnLeagueScope("tennis", "wta", "WTA Tennis", "Tennis"),

            // Combat Sports
            EspnLeagueScope("mma", "ufc", "UFC / MMA", "MMA"),
            EspnLeagueScope("mma", "pfl", "PFL MMA", "MMA"),
            EspnLeagueScope("boxing", "boxing", "Boxing", "Boxing"),

            // Golf
            EspnLeagueScope("golf", "pga", "PGA Tour", "Golf"),
            EspnLeagueScope("golf", "lpga", "LPGA Tour", "Golf"),
            EspnLeagueScope("golf", "liv", "LIV Golf", "Golf"),
            EspnLeagueScope("golf", "eur", "DP World Tour", "Golf"),

            // Cricket (Scorepanel)
            EspnLeagueScope(
                sportPath = "cricket",
                leagueCode = "scorepanel",
                displayName = "Cricket (T20, ODIs, Test Series)",
                genericSport = "Cricket",
                customEndpoint = CRICKET_PANEL_ENDPOINT,
            ),

            // Rugby (Scorepanel)
            EspnLeagueScope(
                sportPath = "rugby",
                leagueCode = "scorepanel",
                displayName = "Rugby (Top 14, Premiership, Internationals)",
                genericSport = "Rugby",
                customEndpoint = RUGBY_PANEL_ENDPOINT,
            ),

            // Australian Football (AFL)
            EspnLeagueScope("australian-football", "afl", "AFL Australian Football", "Football"),

            // Volleyball
            EspnLeagueScope("volleyball", "mens-college-volleyball", "Men's College Volleyball", "Volleyball"),
            EspnLeagueScope("volleyball", "womens-college-volleyball", "Women's College Volleyball", "Volleyball"),

            // Lacrosse
            EspnLeagueScope("lacrosse", "pll", "PLL Lacrosse", "Other"),
            EspnLeagueScope("lacrosse", "nll", "NLL Lacrosse", "Other"),

            // Wrestling & Championship Boxing (from zero-auth daily verified mirror)
            EspnLeagueScope(
                sportPath = "wrestling",
                leagueCode = "wwe",
                displayName = "WWE (Raw, SmackDown, NXT, PLE)",
                genericSport = "Wrestling",
                customEndpoint = WRESTLING_ENDPOINT,
            ),
            EspnLeagueScope(
                sportPath = "wrestling",
                leagueCode = "aew",
                displayName = "AEW (Dynamite, Collision, PPV)",
                genericSport = "Wrestling",
                customEndpoint = WRESTLING_ENDPOINT,
            ),
            EspnLeagueScope(
                sportPath = "wrestling",
                leagueCode = "tna",
                displayName = "TNA Wrestling",
                genericSport = "Wrestling",
                customEndpoint = WRESTLING_ENDPOINT,
            ),
            EspnLeagueScope(
                sportPath = "wrestling",
                leagueCode = "major-boxing",
                displayName = "Championship Boxing Cards",
                genericSport = "Boxing",
                customEndpoint = WRESTLING_ENDPOINT,
            ),
        )

        val DEFAULT_SCOPES: List<EspnLeagueScope> = ALL_SCOPES
    }
}
