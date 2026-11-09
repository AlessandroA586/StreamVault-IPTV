package com.streamvault.feature.live.sports

import java.text.Normalizer
import java.util.Locale

internal object SportsTextNormalizer {
    private val combiningMarkPattern = Regex("\\p{Mn}+")
    private val nonLetterOrNumberPattern = Regex("[^\\p{L}\\p{N}]+")
    private val whitespacePattern = Regex("\\s+")

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(combiningMarkPattern, "")
        .lowercase(Locale.ROOT)
        .replace('&', ' ')
        .replace(nonLetterOrNumberPattern, " ")
        .replace(whitespacePattern, " ")
        .trim()
}

internal object SportsAliasRegistry {
    private val clubPrefixPattern = Regex("\\b(?:fc|cf|sc|ac|club)\\b")
    private val nonNameWordPattern = Regex("\\b(?:football|basketball|the)\\b")
    private val whitespacePattern = Regex("\\s+")
    private val aliases = mapOf(
        "barca" to "barcelona",
        "barca fc" to "barcelona",
        "man utd" to "manchester united",
        "man united" to "manchester united",
        "manchester utd" to "manchester united",
        "man u" to "manchester united",
        "mufc" to "manchester united",
        "manchester united" to "manchester united",
        "man city" to "manchester city",
        "manchester city" to "manchester city",
        "atleti" to "atletico madrid",
        "atletico de madrid" to "atletico madrid",
        "atletico madrid" to "atletico madrid",
        "psg" to "paris saint germain",
        "paris sg" to "paris saint germain",
        "paris saint germain" to "paris saint germain",
        "bayern" to "bayern munich",
        "bayern munchen" to "bayern munich",
        "bayern munich" to "bayern munich",
        "borussia dortmund" to "borussia dortmund",
        "bvb" to "borussia dortmund",
        "juve" to "juventus",
        "juventus" to "juventus",
        "inter milan" to "inter milan",
        "la lakers" to "los angeles lakers",
        "los angeles lakers" to "los angeles lakers",
        "gs warriors" to "golden state warriors",
        "golden state" to "golden state warriors",
        "golden state warriors" to "golden state warriors",
    )

    fun normalizeTeamName(value: String): String {
        val normalized = SportsTextNormalizer.normalize(value)
            .replace(clubPrefixPattern, " ")
            .replace(nonNameWordPattern, " ")
            .replace(whitespacePattern, " ")
            .trim()
        return aliases[normalized] ?: normalized
    }

    fun aliasesForCanonical(value: String): Set<String> {
        val canonical = normalizeTeamName(value)
        return buildSet {
            add(canonical)
            aliases.forEach { (alias, target) -> if (target == canonical) add(alias) }
        }
    }
}

internal data class SportsLeague(
    val id: String,
    val displayName: String,
    val sport: SportType,
    val aliases: Set<String>,
)

internal object SportsLeagueRegistry {
    private data class Definition(
        val displayName: String,
        val sport: SportType,
        val aliases: List<String>,
    )

    private val definitions = listOf(
        Definition("UEFA Champions League", SportType.FOOTBALL, listOf("UEFA Champions League", "Champions League", "UCL")),
        Definition("UEFA Europa League", SportType.FOOTBALL, listOf("UEFA Europa League", "Europa League", "UEL")),
        Definition(
            "UEFA Conference League",
            SportType.FOOTBALL,
            listOf("UEFA Conference League", "UEFA Europa Conference League", "Conference League", "UECL"),
        ),
        Definition(
            "EFL Championship",
            SportType.FOOTBALL,
            listOf(
                "EFL Championship", "English Football League Championship",
                "Sky Bet Championship", "SkyBet Championship",
                "EFL English Football League SkyBet Championship",
            ),
        ),
        Definition("Premier League", SportType.FOOTBALL, listOf("Premier League", "English Premier League", "EPL")),
        Definition("La Liga", SportType.FOOTBALL, listOf("La Liga", "LaLiga")),
        Definition("Serie A", SportType.FOOTBALL, listOf("Serie A")),
        // Série B precedes Série A: equal longest aliases keep list order, and Série A owns the bare "Brasileirão".
        Definition(
            "Brasileirão Série B",
            SportType.FOOTBALL,
            listOf(
                "Brasileirão Série B", "Campeonato Brasileiro Série B", "Série B do Brasileirão",
                "Série B do futebol masculino brasileiro",
            ),
        ),
        Definition(
            "Brasileirão Série A",
            SportType.FOOTBALL,
            listOf(
                "Brasileirão Série A", "Brasileirão", "Campeonato Brasileiro Série A",
                "Série A do Brasileirão", "Série A do futebol masculino brasileiro",
            ),
        ),
        Definition("Copa do Brasil", SportType.FOOTBALL, listOf("Copa do Brasil")),
        Definition(
            "Copa Libertadores",
            SportType.FOOTBALL,
            listOf("Copa Libertadores", "CONMEBOL Libertadores", "Libertadores"),
        ),
        Definition(
            "Copa Sul-Americana",
            SportType.FOOTBALL,
            listOf("Copa Sul-Americana", "Sul-Americana", "Sulamericana", "Copa Sudamericana", "CONMEBOL Sudamericana"),
        ),
        Definition("Bundesliga", SportType.FOOTBALL, listOf("Bundesliga", "1 Bundesliga")),
        Definition("Ligue 1", SportType.FOOTBALL, listOf("Ligue 1")),
        Definition("FA Cup", SportType.FOOTBALL, listOf("FA Cup")),
        Definition("Eredivisie", SportType.FOOTBALL, listOf("Eredivisie")),
        Definition("Primeira Liga", SportType.FOOTBALL, listOf("Primeira Liga")),
        Definition("Major League Soccer", SportType.FOOTBALL, listOf("Major League Soccer", "MLS")),
        Definition("NBA", SportType.BASKETBALL, listOf("NBA")),
        Definition("WNBA", SportType.BASKETBALL, listOf("WNBA")),
        Definition("EuroLeague", SportType.BASKETBALL, listOf("EuroLeague", "Euro League")),
        Definition("LNB Pro A", SportType.BASKETBALL, listOf("LNB Pro A")),
        Definition("NFL", SportType.AMERICAN_FOOTBALL, listOf("NFL")),
        Definition("MLB", SportType.BASEBALL, listOf("MLB")),
        Definition("NHL", SportType.HOCKEY, listOf("NHL")),
        Definition("ATP Tour", SportType.TENNIS, listOf("ATP Tour", "ATP Masters")),
        Definition("WTA Tour", SportType.TENNIS, listOf("WTA Tour")),
        Definition("Wimbledon", SportType.TENNIS, listOf("Wimbledon")),
        Definition("Formula 1", SportType.MOTORSPORT, listOf("Formula 1", "Formula One", "F1")),
        Definition("NASCAR", SportType.MOTORSPORT, listOf("NASCAR")),
        Definition("MotoGP", SportType.MOTORSPORT, listOf("MotoGP")),
        Definition("UFC", SportType.MMA, listOf("UFC")),
        Definition("PGA Tour", SportType.GOLF, listOf("PGA Tour")),
        Definition("LPGA Tour", SportType.GOLF, listOf("LPGA Tour")),
        Definition("Indian Premier League", SportType.CRICKET, listOf("Indian Premier League", "IPL")),
        Definition("Six Nations", SportType.RUGBY, listOf("Six Nations", "Six Nations Championship")),
        Definition("World Surf League", SportType.OTHER, listOf("World Surf League", "WSL")),
    ).map { definition ->
        definition to definition.aliases.map(SportsTextNormalizer::normalize)
    }.sortedByDescending { (_, aliases) -> aliases.maxOf(String::length) }

    fun detect(text: String): SportsLeague? {
        val normalizedText = " ${SportsTextNormalizer.normalize(text)} "
        val definition = definitions.firstOrNull { (_, aliases) ->
            aliases.any { alias -> normalizedText.contains(" $alias ") }
        } ?: return null
        return definition.toLeague()
    }

    fun detectNormalized(normalizedText: String): SportsLeague? {
        val searchableText = " " + normalizedText + " "
        val definition = definitions.firstOrNull { (_, aliases) ->
            aliases.any { alias -> searchableText.contains(" $alias ") }
        } ?: return null
        return definition.toLeague()
    }

    fun aliasesFor(displayName: String?): Set<String> {
        val normalized = displayName?.let(SportsTextNormalizer::normalize).orEmpty()
        if (normalized.isBlank()) return emptySet()
        return definitions.firstOrNull { (definition, aliases) ->
            SportsTextNormalizer.normalize(definition.displayName) == normalized ||
                aliases.any { alias -> alias == normalized }
        }?.toLeague()?.aliases.orEmpty()
    }

    private fun Pair<Definition, List<String>>.toLeague(): SportsLeague {
        val definition = first
        return SportsLeague(
            id = SportsTextNormalizer.normalize(definition.displayName).replace(' ', '-'),
            displayName = definition.displayName,
            sport = definition.sport,
            aliases = second.toSet(),
        )
    }
}

internal object SportsLeagueBadgeRegistry {
    private const val ESPN_SOCCER_BASE = "https://a.espncdn.com/i/leaguelogos/soccer/500"
    private const val ESPN_LEAGUE_BASE = "https://a.espncdn.com/i/teamlogos/leagues/500"

    private val badges = mapOf(
        // Football / Soccer
        "premier-league" to "$ESPN_SOCCER_BASE/23.png",
        "eng.1" to "$ESPN_SOCCER_BASE/23.png",
        "epl" to "$ESPN_SOCCER_BASE/23.png",
        "la-liga" to "$ESPN_SOCCER_BASE/15.png",
        "esp.1" to "$ESPN_SOCCER_BASE/15.png",
        "laliga" to "$ESPN_SOCCER_BASE/15.png",
        "serie-a" to "$ESPN_SOCCER_BASE/12.png",
        "ita.1" to "$ESPN_SOCCER_BASE/12.png",
        "bundesliga" to "$ESPN_SOCCER_BASE/10.png",
        "ger.1" to "$ESPN_SOCCER_BASE/10.png",
        "ligue-1" to "$ESPN_SOCCER_BASE/9.png",
        "fra.1" to "$ESPN_SOCCER_BASE/9.png",
        "uefa-champions-league" to "$ESPN_SOCCER_BASE/2.png",
        "champions-league" to "$ESPN_SOCCER_BASE/2.png",
        "uefa.champions" to "$ESPN_SOCCER_BASE/2.png",
        "ucl" to "$ESPN_SOCCER_BASE/2.png",
        "uefa-europa-league" to "$ESPN_SOCCER_BASE/2035.png",
        "europa-league" to "$ESPN_SOCCER_BASE/2035.png",
        "uefa.europa" to "$ESPN_SOCCER_BASE/2035.png",
        "uel" to "$ESPN_SOCCER_BASE/2035.png",
        "uefa-conference-league" to "$ESPN_SOCCER_BASE/2085.png",
        "conference-league" to "$ESPN_SOCCER_BASE/2085.png",
        "uefa.europa.conf" to "$ESPN_SOCCER_BASE/2085.png",
        "eredivisie" to "$ESPN_SOCCER_BASE/11.png",
        "ned.1" to "$ESPN_SOCCER_BASE/11.png",
        "primeira-liga" to "$ESPN_SOCCER_BASE/14.png",
        "por.1" to "$ESPN_SOCCER_BASE/14.png",
        "major-league-soccer" to "$ESPN_SOCCER_BASE/19.png",
        "mls" to "$ESPN_SOCCER_BASE/19.png",
        "usa.1" to "$ESPN_SOCCER_BASE/19.png",
        "copa-libertadores" to "$ESPN_SOCCER_BASE/24.png",
        "conmebol.libertadores" to "$ESPN_SOCCER_BASE/24.png",
        "copa-sudamericana" to "$ESPN_SOCCER_BASE/25.png",
        "copa-sul-americana" to "$ESPN_SOCCER_BASE/25.png",
        "conmebol.sudamericana" to "$ESPN_SOCCER_BASE/25.png",
        "liga-mx" to "$ESPN_SOCCER_BASE/22.png",
        "mex.1" to "$ESPN_SOCCER_BASE/22.png",
        "brasileirao-serie-a" to "$ESPN_SOCCER_BASE/85.png",
        "brasileirao" to "$ESPN_SOCCER_BASE/85.png",
        "bra.1" to "$ESPN_SOCCER_BASE/85.png",
        "arg.1" to "$ESPN_SOCCER_BASE/1.png",
        "col.1" to "$ESPN_SOCCER_BASE/107.png",
        // Basketball
        "nba" to "$ESPN_LEAGUE_BASE/nba.png",
        "wnba" to "$ESPN_LEAGUE_BASE/wnba.png",
        // American Football
        "nfl" to "$ESPN_LEAGUE_BASE/nfl.png",
        // Baseball
        "mlb" to "$ESPN_LEAGUE_BASE/mlb.png",
        // Hockey
        "nhl" to "$ESPN_LEAGUE_BASE/nhl.png",
        // Motorsport
        "formula-1" to "$ESPN_LEAGUE_BASE/f1.png",
        "formula-one" to "$ESPN_LEAGUE_BASE/f1.png",
        "f1" to "$ESPN_LEAGUE_BASE/f1.png",
        "motogp" to "$ESPN_LEAGUE_BASE/motogp.png",
        "nascar" to "$ESPN_LEAGUE_BASE/nascar.png",
        "indycar" to "$ESPN_LEAGUE_BASE/indycar.png",
        // Tennis
        "atp-tour" to "$ESPN_LEAGUE_BASE/atp.png",
        "atp" to "$ESPN_LEAGUE_BASE/atp.png",
        "wta-tour" to "$ESPN_LEAGUE_BASE/wta.png",
        "wta" to "$ESPN_LEAGUE_BASE/wta.png",
        // MMA & Combat
        "ufc" to "$ESPN_LEAGUE_BASE/ufc.png",
        "boxing" to "$ESPN_LEAGUE_BASE/boxing.png",
        // Golf
        "pga-tour" to "$ESPN_LEAGUE_BASE/pga.png",
        "pga" to "$ESPN_LEAGUE_BASE/pga.png",
        "lpga-tour" to "$ESPN_LEAGUE_BASE/lpga.png",
        // Cricket
        "indian-premier-league" to "$ESPN_LEAGUE_BASE/ipl.png",
        "ipl" to "$ESPN_LEAGUE_BASE/ipl.png",
    )

    fun badgeUrl(leagueKeyOrName: String?): String? {
        if (leagueKeyOrName.isNullOrBlank()) return null
        val raw = leagueKeyOrName.trim()
        val direct = badges[raw.lowercase(Locale.ROOT)]
        if (direct != null) return direct

        val normalized = SportsTextNormalizer.normalize(raw).replace(' ', '-')
        return badges[normalized]
            ?: badges[normalized.removePrefix("league-")]
            ?: badges[normalized.substringAfterLast('/')]
    }
}
