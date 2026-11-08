package com.streamvault.data.sports.espn

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.streamvault.data.sports.provider.SportsProviderHttpClient
import com.streamvault.domain.sports.provider.*
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class EspnLeagueScope(
    val sportPath: String,
    val leagueCode: String,
    val displayName: String,
    val genericSport: String,
    val customEndpoint: String? = null,
) {
    val fullPath: String get() = if (sportPath.isEmpty()) leagueCode else "$sportPath/$leagueCode"
    val safeFileId: String get() = "${sportPath}_${leagueCode}".replace(Regex("[^a-zA-Z0-9]+"), "_")
}

private class IntRef(var value: Int = 0)

internal class EspnSportsProvider(
    private val config: SportsProviderConfig,
    override val descriptor: SportsProviderDescriptor,
    private val httpClient: SportsProviderHttpClient,
    private val cache: EspnResponseCache,
    private val clock: Clock = Clock.systemUTC(),
) : EventProvider, LiveScoreProvider, GameStatusProvider, StandingsProvider,
    ArtworkProvider, TeamMetadataProvider, BroadcastProvider, SportsProviderConnectionTester {

    override val instanceId: String = config.instanceId

    // Team and brand cache for artwork and metadata
    private val knownTeams = ConcurrentHashMap<String, ProviderCompetitor>()

    override suspend fun testConnection(credentials: SportsProviderCredentials?): SportsProviderResult<Unit> {
        val testUrl = "${EspnSportsProviderFactory.BASE_URL}/soccer/eng.1/scoreboard?limit=1".toHttpUrlOrNull()
            ?: return invalidConfiguration()
        return when (val res = httpClient.get(testUrl, SportsProviderAuth.None, null) { it }) {
            is SportsProviderResult.Failure -> res
            is SportsProviderResult.Success -> {
                if (parseObject(res.value) != null) SportsProviderResult.Success(Unit)
                else invalidResponse()
            }
        }
    }

    override suspend fun events(request: ProviderEventRequest): SportsProviderResult<ProviderBatch<ProviderEvent>> {
        val scopes = resolveScopes(request.sport, request.leagueId)
        if (scopes.isEmpty()) return SportsProviderResult.Success(ProviderBatch(emptyList()))

        val allEvents = mutableListOf<ProviderEvent>()
        val rejectedRef = IntRef(0)

        val fromDate = request.from.atZone(ZoneOffset.UTC).toLocalDate()
        val untilDate = request.until.atZone(ZoneOffset.UTC).toLocalDate()
        val dateParam = if (fromDate == untilDate) {
            fromDate.format(DATE_FORMATTER)
        } else {
            "${fromDate.format(DATE_FORMATTER)}-${untilDate.format(DATE_FORMATTER)}"
        }

        for (scope in scopes) {
            if (scope.sportPath == "wrestling") {
                fetchWrestlingEvents(scope, request, allEvents)
                continue
            }

            if (scope.customEndpoint != null) {
                fetchScorepanelEvents(scope, request, allEvents, rejectedRef)
                continue
            }

            val endpoint = "${EspnSportsProviderFactory.BASE_URL}/${scope.fullPath}/scoreboard?dates=$dateParam&limit=250"
            val raw = rawJson(endpoint, FIXTURE_TTL)
            val jsonText = when (raw) {
                is SportsProviderResult.Success -> raw.value
                is SportsProviderResult.Failure -> {
                    // Fallback to nuvio-data mirror
                    val mirrorUrl = "${EspnSportsProviderFactory.NUVIO_DATA_MIRROR}/espn/${scope.safeFileId}.json"
                    when (val mirrorRes = rawJson(mirrorUrl, FIXTURE_TTL)) {
                        is SportsProviderResult.Success -> mirrorRes.value
                        is SportsProviderResult.Failure -> continue
                    }
                }
            }

            val root = parseObject(jsonText) ?: continue
            val eventsArray = root.arrayOrNull("events") ?: continue
            for (i in 0 until eventsArray.size()) {
                val evObj = eventsArray[i].asObjectOrNull() ?: continue
                val event = mapEvent(evObj, scope)
                if (event != null) {
                    val start = event.eventStart
                    if (start == null || (!start.isBefore(request.from) && start.isBefore(request.until))) {
                        allEvents += event
                        event.competitors.forEach { comp ->
                            comp.id?.let { knownTeams[it] = comp }
                        }
                    }
                } else {
                    rejectedRef.value++
                }
            }
        }

        return SportsProviderResult.Success(ProviderBatch(allEvents, rejectedRef.value))
    }

    override suspend fun liveScores(request: ProviderScoreRequest): SportsProviderResult<ProviderBatch<ProviderScore>> {
        val today = clock.instant().atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMATTER)
        val scopes = configuredScopes()
        val scores = mutableListOf<ProviderScore>()
        var rejected = 0

        for (scope in scopes) {
            if (scope.sportPath == "wrestling") {
                fetchWrestlingScores(scope, scores)
                continue
            }

            if (scope.customEndpoint != null) {
                fetchScorepanelScores(scope, scores)
                continue
            }

            val endpoint = "${EspnSportsProviderFactory.BASE_URL}/${scope.fullPath}/scoreboard?dates=$today&limit=250"
            val raw = rawJson(endpoint, LIVE_SCORE_TTL)
            val jsonText = when (raw) {
                is SportsProviderResult.Success -> raw.value
                is SportsProviderResult.Failure -> {
                    val mirrorUrl = "${EspnSportsProviderFactory.NUVIO_DATA_MIRROR}/espn/${scope.safeFileId}.json"
                    when (val mirrorRes = rawJson(mirrorUrl, LIVE_SCORE_TTL)) {
                        is SportsProviderResult.Success -> mirrorRes.value
                        is SportsProviderResult.Failure -> continue
                    }
                }
            }

            val root = parseObject(jsonText) ?: continue
            val eventsArray = root.arrayOrNull("events") ?: continue
            for (i in 0 until eventsArray.size()) {
                val evObj = eventsArray[i].asObjectOrNull() ?: continue
                val score = mapScore(evObj)
                if (score != null) {
                    scores += score
                } else {
                    rejected++
                }
            }
        }

        return SportsProviderResult.Success(ProviderBatch(scores, rejected))
    }

    override suspend fun gameStatuses(request: ProviderScoreRequest): SportsProviderResult<ProviderBatch<ProviderEventStatus>> {
        return when (val liveResult = liveScores(request)) {
            is SportsProviderResult.Failure -> liveResult
            is SportsProviderResult.Success -> {
                val statuses = liveResult.value.items.mapNotNull { score ->
                    score.status?.let { ProviderEventStatus(score.providerEventId, it) }
                }
                SportsProviderResult.Success(ProviderBatch(statuses, liveResult.value.rejectedItemCount))
            }
        }
    }

    override suspend fun standings(request: ProviderStandingsRequest): SportsProviderResult<ProviderStandings> {
        val scope = resolveScope(request.sport, request.leagueId) ?: return invalidConfiguration()
        val url = "https://site.api.espn.com/apis/v2/sports/${scope.fullPath}/standings"
        val raw = rawJson(url, TABLE_TTL)
        return when (raw) {
            is SportsProviderResult.Failure -> raw
            is SportsProviderResult.Success -> {
                val standings = mapStandings(raw.value, scope) ?: invalidResponse()
                if (standings is ProviderStandings) SportsProviderResult.Success(standings)
                else invalidResponse()
            }
        }
    }

    override suspend fun artwork(request: ProviderEntityRef): SportsProviderResult<ProviderBatch<ProviderArtwork>> {
        val team = knownTeams[request.id]
        val logoUrl = team?.logoUrl
        return if (logoUrl != null && logoUrl.startsWith("https://")) {
            val artwork = ProviderArtwork(
                entity = request,
                uri = URI(logoUrl),
                kind = ProviderArtworkKind.LOGO,
            )
            SportsProviderResult.Success(ProviderBatch(listOf(artwork)))
        } else {
            SportsProviderResult.Success(ProviderBatch(emptyList()))
        }
    }

    override suspend fun teamMetadata(request: ProviderTeamMetadataRequest): SportsProviderResult<ProviderTeamMetadata> {
        val team = knownTeams[request.teamId]
            ?: ProviderCompetitor(name = request.teamId, id = request.teamId, kind = ProviderCompetitorKind.TEAM)
        return SportsProviderResult.Success(
            ProviderTeamMetadata(team = team, shortName = team.name.take(3).uppercase())
        )
    }

    override suspend fun broadcasters(request: ProviderEventDetailRequest): SportsProviderResult<ProviderBatch<ProviderBroadcaster>> {
        return SportsProviderResult.Success(ProviderBatch(emptyList()))
    }

    private suspend fun fetchWrestlingEvents(
        scope: EspnLeagueScope,
        request: ProviderEventRequest,
        allEvents: MutableList<ProviderEvent>,
    ) {
        val endpoint = scope.customEndpoint ?: return
        val raw = rawJson(endpoint, FIXTURE_TTL)
        val jsonText = when (raw) {
            is SportsProviderResult.Success -> raw.value
            is SportsProviderResult.Failure -> return
        }
        val root = parseObject(jsonText) ?: return

        val leagueBadges = mutableMapOf<String, String>()
        val leagueNames = mutableMapOf<String, String>()
        val brandBadges = mutableMapOf<String, String>()

        root.arrayOrNull("leagues")?.forEach { lItem ->
            val lObj = lItem.asObjectOrNull() ?: return@forEach
            val key = lObj.stringOrNull("key") ?: return@forEach
            val name = lObj.stringOrNull("name") ?: key
            val badge = lObj.stringOrNull("badge")
            if (badge != null && badge.startsWith("https://")) {
                leagueBadges[key] = badge
            }
            leagueNames[key] = name

            lObj.arrayOrNull("brands")?.forEach { bItem ->
                val bObj = bItem.asObjectOrNull() ?: return@forEach
                val word = bObj.stringOrNull("word")?.lowercase(Locale.ROOT)?.trim() ?: return@forEach
                val bBadge = bObj.stringOrNull("badge")
                if (bBadge != null && bBadge.startsWith("https://")) {
                    brandBadges["$key|$word"] = bBadge
                }
            }
        }

        val now = clock.instant()
        val targetKey = if (scope.leagueCode == "major-boxing") "boxing" else scope.leagueCode

        val eventsArray = root.arrayOrNull("events") ?: return
        for (i in 0 until eventsArray.size()) {
            val evObj = eventsArray[i].asObjectOrNull() ?: continue
            val evLeague = evObj.stringOrNull("league") ?: continue
            if (!evLeague.equals(targetKey, ignoreCase = true)) continue

            val id = evObj.stringOrNull("id") ?: continue
            val rawName = evObj.stringOrNull("name") ?: continue
            val name = cleanWrestlingTitle(rawName)
            val startStr = evObj.stringOrNull("start") ?: continue
            val start = startStr.toInstantOrNull() ?: continue
            val end = start.plus(Duration.ofHours(3))

            if (start.isBefore(request.from) || !start.isBefore(request.until)) continue

            val state = when {
                now.isAfter(end) -> ProviderGameState.FINAL
                !now.isBefore(start) -> ProviderGameState.LIVE
                else -> ProviderGameState.SCHEDULED
            }
            val status = ProviderGameStatus(
                state = state,
                detail = listOfNotNull(evObj.stringOrNull("venue"), evObj.stringOrNull("country")).joinToString(" · ").ifBlank { null },
                updatedAt = now,
            )

            val lowerName = name.lowercase(Locale.ROOT)
            var matchedLogo = leagueBadges[evLeague]
            brandBadges.forEach { (kw, bLogo) ->
                val brandWord = kw.substringAfter("|")
                if (lowerName.contains(brandWord)) {
                    matchedLogo = bLogo
                }
            }

            val competitor = ProviderCompetitor(
                name = name,
                id = "wrestling_${evLeague}_$id",
                kind = ProviderCompetitorKind.TEAM,
                role = ProviderCompetitorRole.HOME,
                logoUrl = matchedLogo,
            )
            competitor.id?.let { knownTeams[it] = competitor }

            val event = ProviderEvent(
                providerEventId = "wrestling_$id",
                sport = scope.genericSport,
                league = ProviderLeague(name = leagueNames[evLeague] ?: scope.displayName, id = evLeague, sport = scope.genericSport),
                title = name,
                competitors = listOf(competitor),
                eventStart = start,
                eventEnd = end,
                status = status,
                broadcasters = emptyList(),
            )
            allEvents += event
        }
    }

    private suspend fun fetchWrestlingScores(
        scope: EspnLeagueScope,
        scores: MutableList<ProviderScore>,
    ) {
        val endpoint = scope.customEndpoint ?: return
        val raw = rawJson(endpoint, LIVE_SCORE_TTL)
        val jsonText = when (raw) {
            is SportsProviderResult.Success -> raw.value
            is SportsProviderResult.Failure -> return
        }
        val root = parseObject(jsonText) ?: return
        val now = clock.instant()
        val targetKey = if (scope.leagueCode == "major-boxing") "boxing" else scope.leagueCode

        val eventsArray = root.arrayOrNull("events") ?: return
        for (i in 0 until eventsArray.size()) {
            val evObj = eventsArray[i].asObjectOrNull() ?: continue
            val evLeague = evObj.stringOrNull("league") ?: continue
            if (!evLeague.equals(targetKey, ignoreCase = true)) continue

            val id = evObj.stringOrNull("id") ?: continue
            val rawName = evObj.stringOrNull("name") ?: continue
            val name = cleanWrestlingTitle(rawName)
            val start = evObj.stringOrNull("start")?.toInstantOrNull() ?: continue
            val end = start.plus(Duration.ofHours(3))

            // Only report scores if happening today / within active window
            val windowStart = now.minus(Duration.ofHours(24))
            val windowEnd = now.plus(Duration.ofHours(24))
            if (start.isBefore(windowStart) || start.isAfter(windowEnd)) continue

            val state = when {
                now.isAfter(end) -> ProviderGameState.FINAL
                !now.isBefore(start) -> ProviderGameState.LIVE
                else -> ProviderGameState.SCHEDULED
            }

            val status = ProviderGameStatus(
                state = state,
                detail = listOfNotNull(evObj.stringOrNull("venue"), evObj.stringOrNull("country")).joinToString(" · ").ifBlank { null },
                updatedAt = now,
            )

            scores += ProviderScore(
                providerEventId = "wrestling_$id",
                scores = listOf(
                    ProviderCompetitorScore(
                        competitor = ProviderCompetitor(
                            name = name,
                            id = "wrestling_${evLeague}_$id",
                            kind = ProviderCompetitorKind.TEAM,
                            role = ProviderCompetitorRole.HOME,
                        ),
                        value = if (state == ProviderGameState.LIVE) "LIVE" else null,
                    )
                ),
                status = status,
                updatedAt = now,
            )
        }
    }

    private suspend fun fetchScorepanelEvents(
        scope: EspnLeagueScope,
        request: ProviderEventRequest,
        allEvents: MutableList<ProviderEvent>,
        rejectedRef: IntRef,
    ) {
        val endpoint = scope.customEndpoint ?: return
        val raw = rawJson(endpoint, FIXTURE_TTL)
        val jsonText = when (raw) {
            is SportsProviderResult.Success -> raw.value
            is SportsProviderResult.Failure -> return
        }
        val root = parseObject(jsonText) ?: return
        val scoresArray = root.arrayOrNull("scores") ?: return

        for (sIndex in 0 until scoresArray.size()) {
            val sObj = scoresArray[sIndex].asObjectOrNull() ?: continue
            val evArray = sObj.arrayOrNull("events") ?: continue
            for (i in 0 until evArray.size()) {
                val evObj = evArray[i].asObjectOrNull() ?: continue
                val event = mapEvent(evObj, scope)
                if (event != null) {
                    val start = event.eventStart
                    if (start == null || (!start.isBefore(request.from) && start.isBefore(request.until))) {
                        allEvents += event
                        event.competitors.forEach { comp ->
                            comp.id?.let { knownTeams[it] = comp }
                        }
                    }
                } else {
                    rejectedRef.value++
                }
            }
        }
    }

    private suspend fun fetchScorepanelScores(
        scope: EspnLeagueScope,
        scores: MutableList<ProviderScore>,
    ) {
        val endpoint = scope.customEndpoint ?: return
        val raw = rawJson(endpoint, LIVE_SCORE_TTL)
        val jsonText = when (raw) {
            is SportsProviderResult.Success -> raw.value
            is SportsProviderResult.Failure -> return
        }
        val root = parseObject(jsonText) ?: return
        val scoresArray = root.arrayOrNull("scores") ?: return

        for (sIndex in 0 until scoresArray.size()) {
            val sObj = scoresArray[sIndex].asObjectOrNull() ?: continue
            val evArray = sObj.arrayOrNull("events") ?: continue
            for (i in 0 until evArray.size()) {
                val evObj = evArray[i].asObjectOrNull() ?: continue
                val score = mapScore(evObj)
                if (score != null) {
                    scores += score
                }
            }
        }
    }

    private fun mapEvent(ev: JsonObject, scope: EspnScope): ProviderEvent? {
        val id = ev.stringOrNull("id") ?: return null
        val title = ev.stringOrNull("name") ?: ev.stringOrNull("shortName")
        val dateStr = ev.stringOrNull("date") ?: return null
        val start = dateStr.toInstantOrNull() ?: return null

        val comp = ev.arrayOrNull("competitions")?.firstOrNull()?.asObjectOrNull()
        val end = ev.stringOrNull("endDate")?.toInstantOrNull()

        // Status mapping
        val st = comp?.objOrNull("status") ?: ev.objOrNull("status")
        val status = mapStatus(st, start)

        // Competitors mapping
        val competitorsList = mutableListOf<ProviderCompetitor>()
        val compArray = comp?.arrayOrNull("competitors")
        if (compArray != null) {
            for (i in 0 until compArray.size()) {
                val cObj = compArray[i].asObjectOrNull() ?: continue
                val teamObj = cObj.objOrNull("team") ?: continue
                val teamName = teamObj.stringOrNull("displayName")
                    ?: teamObj.stringOrNull("name") ?: continue
                val teamId = teamObj.stringOrNull("id")
                val isHome = cObj.stringOrNull("homeAway") == "home"
                val logo = teamObj.stringOrNull("logo")
                    ?: teamObj.arrayOrNull("logos")?.firstOrNull()?.asObjectOrNull()?.stringOrNull("href")

                val competitor = ProviderCompetitor(
                    name = teamName,
                    id = teamId,
                    kind = ProviderCompetitorKind.TEAM,
                    role = if (isHome) ProviderCompetitorRole.HOME else ProviderCompetitorRole.AWAY,
                    logoUrl = logo?.takeIf { it.startsWith("https://") },
                )
                competitorsList += competitor
            }
        }

        // Broadcasters mapping
        val broadcastersList = mutableListOf<ProviderBroadcaster>()
        comp?.arrayOrNull("broadcasts")?.forEach { bItem ->
            val bObj = bItem.asObjectOrNull() ?: return@forEach
            bObj.arrayOrNull("names")?.forEach { nameItem ->
                val name = nameItem.asStringOrNull()?.trim()
                if (!name.isNullOrBlank()) {
                    broadcastersList += ProviderBroadcaster(name = name)
                }
            }
        }
        comp?.arrayOrNull("geoBroadcasts")?.forEach { bItem ->
            val bObj = bItem.asObjectOrNull() ?: return@forEach
            val shortName = bObj.objOrNull("media")?.stringOrNull("shortName")?.trim()
            if (!shortName.isNullOrBlank() && broadcastersList.none { it.name.equals(shortName, ignoreCase = true) }) {
                broadcastersList += ProviderBroadcaster(name = shortName)
            }
        }

        return ProviderEvent(
            providerEventId = id,
            sport = scope.genericSport,
            league = ProviderLeague(name = scope.displayName, id = scope.leagueCode, sport = scope.genericSport),
            title = title,
            competitors = competitorsList,
            eventStart = start,
            eventEnd = end,
            status = status,
            broadcasters = broadcastersList,
        )
    }

    private fun mapScore(ev: JsonObject): ProviderScore? {
        val id = ev.stringOrNull("id") ?: return null
        val comp = ev.arrayOrNull("competitions")?.firstOrNull()?.asObjectOrNull()
        val st = comp?.objOrNull("status") ?: ev.objOrNull("status")
        val status = mapStatus(st, clock.instant())

        val scores = mutableListOf<ProviderCompetitorScore>()
        val compArray = comp?.arrayOrNull("competitors")
        if (compArray != null) {
            for (i in 0 until compArray.size()) {
                val cObj = compArray[i].asObjectOrNull() ?: continue
                val teamObj = cObj.objOrNull("team") ?: continue
                val teamName = teamObj.stringOrNull("displayName") ?: teamObj.stringOrNull("name") ?: continue
                val teamId = teamObj.stringOrNull("id")
                val isHome = cObj.stringOrNull("homeAway") == "home"
                val scoreVal = cObj.stringOrNull("score")

                val competitor = ProviderCompetitor(
                    name = teamName,
                    id = teamId,
                    kind = ProviderCompetitorKind.TEAM,
                    role = if (isHome) ProviderCompetitorRole.HOME else ProviderCompetitorRole.AWAY,
                )
                scores += ProviderCompetitorScore(competitor = competitor, value = scoreVal)
            }
        }

        return ProviderScore(
            providerEventId = id,
            scores = scores,
            status = status,
            updatedAt = clock.instant(),
        )
    }

    private fun mapStatus(st: JsonObject?, startFallback: Instant?): ProviderGameStatus {
        if (st == null) return ProviderGameStatus(state = ProviderGameState.UNKNOWN)
        val type = st.objOrNull("type")
        val stateRaw = type?.stringOrNull("state")
        val nameRaw = type?.stringOrNull("name").orEmpty()
        val displayClock = st.stringOrNull("displayClock")
        val period = st.intOrNull("period")?.toString()
        val detail = type?.stringOrNull("shortDetail") ?: type?.stringOrNull("detail")

        val state = when {
            Regex("POSTPONED|CANCEL|SUSPENDED|FORFEIT|ABANDON").containsMatchIn(nameRaw) -> ProviderGameState.POSTPONED
            stateRaw == "in" -> ProviderGameState.LIVE
            stateRaw == "post" -> ProviderGameState.FINAL
            stateRaw == "pre" -> ProviderGameState.SCHEDULED
            else -> ProviderGameState.UNKNOWN
        }

        return ProviderGameStatus(
            state = state,
            clock = displayClock,
            period = period,
            detail = detail,
            updatedAt = clock.instant(),
        )
    }

    private fun mapStandings(rawJson: String, scope: EspnScope): ProviderStandings? {
        val root = parseObject(rawJson) ?: return null
        val children = root.arrayOrNull("children") ?: return null
        val firstChild = children.firstOrNull()?.asObjectOrNull() ?: return null
        val standingsObj = firstChild.objOrNull("standings") ?: return null
        val entries = standingsObj.arrayOrNull("entries") ?: return null

        val columns = listOf(
            ProviderStandingColumn("GP", "GP"),
            ProviderStandingColumn("W", "W"),
            ProviderStandingColumn("D", "D"),
            ProviderStandingColumn("L", "L"),
            ProviderStandingColumn("GD", "GD"),
            ProviderStandingColumn("PTS", "PTS"),
        )

        val rows = mutableListOf<ProviderStandingRow>()
        for (i in 0 until entries.size()) {
            val entry = entries[i].asObjectOrNull() ?: continue
            val teamObj = entry.objOrNull("team") ?: continue
            val teamName = teamObj.stringOrNull("displayName") ?: teamObj.stringOrNull("name") ?: continue
            val teamId = teamObj.stringOrNull("id")
            val logo = teamObj.arrayOrNull("logos")?.firstOrNull()?.asObjectOrNull()?.stringOrNull("href")

            val statsMap = mutableMapOf<String, String?>()
            entry.arrayOrNull("stats")?.forEach { sItem ->
                val sObj = sItem.asObjectOrNull() ?: return@forEach
                val sName = sObj.stringOrNull("name") ?: sObj.stringOrNull("shortDisplayName") ?: return@forEach
                val sVal = sObj.stringOrNull("displayValue") ?: sObj.intOrNull("value")?.toString()
                when (sName.lowercase(Locale.ROOT)) {
                    "gamesplayed", "gp" -> statsMap["GP"] = sVal
                    "wins", "w" -> statsMap["W"] = sVal
                    "ties", "draws", "d" -> statsMap["D"] = sVal
                    "losses", "l" -> statsMap["L"] = sVal
                    "pointdifferential", "goaldifference", "gd" -> statsMap["GD"] = sVal
                    "points", "pts", "p" -> statsMap["PTS"] = sVal
                }
            }

            val competitor = ProviderCompetitor(
                name = teamName,
                id = teamId,
                kind = ProviderCompetitorKind.TEAM,
                logoUrl = logo?.takeIf { it.startsWith("https://") },
            )
            rows += ProviderStandingRow(
                competitor = competitor,
                rank = i + 1,
                values = statsMap,
            )
        }

        return ProviderStandings(
            league = ProviderLeague(scope.displayName, scope.leagueCode, scope.genericSport),
            groups = listOf(ProviderStandingGroup(name = null, columns = columns, rows = rows)),
            season = firstChild.stringOrNull("name"),
            updatedAt = clock.instant(),
        )
    }

    private suspend fun rawJson(urlStr: String, ttl: Duration): SportsProviderResult<String> {
        val key = "${config.instanceId}|${config.cacheRevision}|$urlStr"
        cache.get(key, clock.instant())?.let { return SportsProviderResult.Success(it) }

        val httpUrl = urlStr.toHttpUrlOrNull() ?: return invalidConfiguration()
        return when (val res = httpClient.get(httpUrl, SportsProviderAuth.None, null) { it }) {
            is SportsProviderResult.Failure -> res
            is SportsProviderResult.Success -> {
                cache.put(key, res.value, ttl, clock.instant())
                res
            }
        }
    }

    private fun configuredScopes(): List<EspnScope> {
        val requestedIds = config.eventLeagueIds.map { it.lowercase(Locale.ROOT) }
        return if (requestedIds.isEmpty()) {
            EspnSportsProviderFactory.DEFAULT_SCOPES
        } else {
            EspnSportsProviderFactory.ALL_SCOPES.filter {
                it.leagueCode.lowercase(Locale.ROOT) in requestedIds ||
                    it.fullPath.lowercase(Locale.ROOT) in requestedIds
            }.ifEmpty { EspnSportsProviderFactory.DEFAULT_SCOPES }
        }
    }

    private fun resolveScopes(sport: String?, leagueId: String?): List<EspnScope> {
        val scopes = configuredScopes()
        return scopes.filter { scope ->
            val sportMatches = sport == null || scope.genericSport.equals(sport, ignoreCase = true) ||
                scope.sportPath.equals(sport, ignoreCase = true)
            val leagueMatches = leagueId == null || scope.leagueCode.equals(leagueId, ignoreCase = true) ||
                scope.fullPath.equals(leagueId, ignoreCase = true) ||
                scope.displayName.contains(leagueId, ignoreCase = true)
            sportMatches && leagueMatches
        }
    }

    private fun resolveScope(sport: String?, leagueId: String?): EspnScope? =
        resolveScopes(sport, leagueId).firstOrNull()

    private fun cleanWrestlingTitle(title: String): String =
        title.replace(Regex("""\s*-\s*[A-Za-z]+\s+\d+(?:st|nd|rd|th)?\s+\d{4}"""), "").trim()

    private fun String.toInstantOrNull(): Instant? =
        runCatching { Instant.parse(this) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(this).toInstant() }.getOrNull()

    private fun parseObject(raw: String): JsonObject? =
        runCatching { JsonParser.parseString(raw) }.getOrNull()?.asObjectOrNull()

    private fun JsonElement.asObjectOrNull(): JsonObject? =
        if (this.isJsonObject) this.asJsonObject else null

    private fun JsonElement.asStringOrNull(): String? =
        if (this.isJsonPrimitive && this.asJsonPrimitive.isString) this.asString else null

    private fun JsonObject.objOrNull(name: String): JsonObject? =
        get(name)?.asObjectOrNull()

    private fun JsonObject.arrayOrNull(name: String): JsonArray? =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.stringOrNull(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.intOrNull(name: String): Int? =
        get(name)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() }

    private fun invalidConfiguration() = SportsProviderResult.Failure(SportsProviderError.INVALID_CONFIGURATION)
    private fun invalidResponse() = SportsProviderResult.Failure(SportsProviderError.INVALID_RESPONSE)

    companion object {
        val FIXTURE_TTL: Duration = Duration.ofMinutes(10)
        val LIVE_SCORE_TTL: Duration = Duration.ofSeconds(45)
        val TABLE_TTL: Duration = Duration.ofHours(1)
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
typealias EspnScope = EspnLeagueScope
