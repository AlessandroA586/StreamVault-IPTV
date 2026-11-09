package com.streamvault.app.update

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GitHubReleaseCheckerTest {
    @Test
    fun parseReleaseAssetSha256DigestAcceptsGitHubDigestShape() {
        val digest = "sha256:ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789"

        assertThat(parseReleaseAssetSha256Digest(digest))
            .isEqualTo("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789")
    }

    @Test
    fun parseReleaseAssetSha256DigestRejectsUnsupportedDigest() {
        assertThat(parseReleaseAssetSha256Digest("sha1:abcdef")).isNull()
        assertThat(parseReleaseAssetSha256Digest("sha256:not-a-hash")).isNull()
        assertThat(parseReleaseAssetSha256Digest("")).isNull()
    }

    @Test
    fun `http failures are returned as errors`() = kotlinx.coroutines.test.runTest {
        val client = mock<OkHttpClient>()
        val call = mock<Call>()
        val response = mock<Response>()
        whenever(client.newCall(any())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(false)
        whenever(response.code).thenReturn(429)

        val result = GitHubReleaseChecker(client).fetchLatestRelease()

        assertThat(result).isInstanceOf(com.streamvault.domain.model.Result.Error::class.java)
        assertThat((result as com.streamvault.domain.model.Result.Error).message).contains("429")
    }

    @Test
    fun `http 403 falls back to web redirect when available`() = kotlinx.coroutines.test.runTest {
        val client = mock<OkHttpClient>()
        val apiCall = mock<Call>()
        val apiResponse = mock<Response>()
        val webCall = mock<Call>()
        val webResponse = mock<Response>()
        val webRequest = okhttp3.Request.Builder()
            .url("https://github.com/AlessandroA586/StreamVault-IPTV/releases/tag/v1.2.4")
            .build()

        whenever(apiResponse.isSuccessful).thenReturn(false)
        whenever(apiResponse.code).thenReturn(403)
        whenever(apiCall.execute()).thenReturn(apiResponse)

        whenever(webResponse.isSuccessful).thenReturn(true)
        whenever(webResponse.request).thenReturn(webRequest)
        whenever(webCall.execute()).thenReturn(webResponse)

        whenever(client.newCall(argThat { url.host == "api.github.com" })).thenReturn(apiCall)
        whenever(client.newCall(argThat { url.host == "github.com" })).thenReturn(webCall)

        val result = GitHubReleaseChecker(client).fetchLatestRelease()

        assertThat(result).isInstanceOf(com.streamvault.domain.model.Result.Success::class.java)
        val data = (result as com.streamvault.domain.model.Result.Success).data
        assertThat(data.versionName).isEqualTo("1.2.4")
        assertThat(data.downloadUrl).contains("StreamVault.apk")
    }

    @Test
    fun `malformed successful response is returned as an error`() = kotlinx.coroutines.test.runTest {
        val client = mock<OkHttpClient>()
        val call = mock<Call>()
        val response = mock<Response>()
        val body = mock<ResponseBody>()
        whenever(client.newCall(any())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(true)
        whenever(response.body).thenReturn(body)
        whenever(body.contentLength()).thenReturn(-1L)
        whenever(body.contentType()).thenReturn(null)
        whenever(body.byteStream()).thenReturn(ByteArrayInputStream("{not-json".toByteArray()))

        val result = GitHubReleaseChecker(client).fetchLatestRelease()

        assertThat(result).isInstanceOf(com.streamvault.domain.model.Result.Error::class.java)
        assertThat((result as com.streamvault.domain.model.Result.Error).message)
            .contains("Update check failed")
    }
}
