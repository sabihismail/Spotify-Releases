package api

import com.sun.net.httpserver.HttpServer
import db.DatabaseImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.SpotifyHttpManager
import util.Config
import util.UrlUtils
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.time.LocalDateTime

object SpotifyImpl {
    private const val redirectUrlPort = 9229
    private const val redirectUrlEndpoint = "spotify-callback"

    private val config = Config.get()
    private val redirectUrl = SpotifyHttpManager.makeUri("http://localhost:$redirectUrlPort/$redirectUrlEndpoint")

    suspend fun get() {
        authenticate()


    }

    private suspend fun authenticate() {
        if (LocalDateTime.now() < DatabaseImpl.expiresIn) return

        val spotifyApiBuilder = SpotifyApi.Builder()
            .setClientId(config.spotifyClientId)
            .setClientSecret(config.spotifyClientSecret)

        if (!DatabaseImpl.accessToken.isNullOrBlank()) {
            spotifyApiBuilder.setAccessToken(DatabaseImpl.accessToken)
                .setRefreshToken(DatabaseImpl.refreshToken)
        }

        val spotifyApi = spotifyApiBuilder.setRedirectUri(redirectUrl)
            .build()

        val queries = runOAuthRequest {
            var uri: URI
            runBlocking {
                uri = spotifyApi.authorizationCodeUri().build().executeAsync().await()
            }
            return@runOAuthRequest uri
        }
        val code = queries["code"]?.first()

        val authorizationWithCode = spotifyApi.authorizationCode(code).build().executeAsync().await()
        DatabaseImpl.saveSpotifyCredentials(authorizationWithCode)
    }

    private suspend fun runOAuthRequest(uriGenerator: () -> URI): Map<String, List<String>> {
        var responseUrl: URI? = null
        val server = HttpServer.create(InetSocketAddress(redirectUrlPort), 0)
        server.createContext("/$redirectUrlEndpoint") { exchange ->
            responseUrl = exchange.requestURI
        }
        server.executor = null
        server.start()

        val uri = uriGenerator()
        withContext(Dispatchers.IO) {
            Desktop.getDesktop().browse(uri)

            while (responseUrl == null) {
                Thread.sleep(100)
            }
        }

        return UrlUtils.splitQuery(responseUrl)
    }
}