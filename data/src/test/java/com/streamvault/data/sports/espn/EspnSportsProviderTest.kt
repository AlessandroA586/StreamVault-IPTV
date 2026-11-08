package com.streamvault.data.sports.espn

import com.google.common.truth.Truth.assertThat
import com.streamvault.data.sports.provider.SportsProviderHttpClient
import com.streamvault.domain.sports.provider.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EspnSportsProviderTest {
    private lateinit var cache: EspnResponseCache
    private val now = Instant.parse("2026-11-08T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Before
    fun setUp() {
        cache = EspnResponseCache()
    }

    private fun factory() = EspnSportsProviderFactory(
        httpClient = SportsProviderHttpClient("TestAgent", clock),
        cache = cache,
    )

    @Test
    fun `descriptor exposes comprehensive sports data and artwork without auth requirements`() {
        val factory = factory()
        assertThat(factory.descriptor.auth).isEqualTo(SportsProviderAuth.None)
        assertThat(factory.descriptor.typeId).isEqualTo(EspnSportsProviderFactory.TYPE_ID)
        assertThat(factory.descriptor.defaultBaseUrl).isEqualTo(EspnSportsProviderFactory.BASE_URL)
        assertThat(factory.descriptor.capabilities).containsExactly(
            SportsCapability.EVENTS,
            SportsCapability.LIVE_SCORES,
            SportsCapability.GAME_STATUS,
            SportsCapability.STANDINGS,
            SportsCapability.ARTWORK,
            SportsCapability.TEAM_METADATA,
            SportsCapability.BROADCASTERS,
        )
        assertThat(factory.descriptor.compliance?.releaseGate).isEqualTo(ProviderReleaseGate.APPROVED)
        assertThat(factory.descriptor.compliance?.artworkStatus).isEqualTo(ArtworkUseStatus.CLEARED)

        // Release policy permits it when attribution is configured
        val registry = SportsProviderRegistry(
            listOf(factory),
            SportsProviderReleasePolicy(attributionSupportedTypes = setOf("espn")),
        )
        assertThat(registry.descriptors).containsExactly(factory.descriptor)
    }

    @Test
    fun `provider creation and scopes resolution are valid`() {
        val factory = factory()
        val config = SportsProviderConfig(instanceId = "espn-default", typeId = "espn")
        val provider = factory.create(config)
        assertThat(provider.instanceId).isEqualTo("espn-default")
        assertThat(provider.descriptor.typeId).isEqualTo("espn")
    }
}
