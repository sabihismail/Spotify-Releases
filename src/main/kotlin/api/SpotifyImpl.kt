package api

import com.sun.net.httpserver.HttpServer
import db.DatabaseImpl
import db.models.GenericKeyValueKey
import db.tables.SpotifyArtistTable
import db.tables.SpotifyPlaylistTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import models.enums.SpotifyStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.SpotifyHttpManager
import se.michaelthelin.spotify.enums.AuthorizationScope
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.model_objects.specification.Track
import util.Config
import util.extensions.UrlExtensions.splitQuery
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.time.LocalDateTime

object SpotifyImpl {
    private const val redirectUrlPort = 9229
    private const val redirectUrlEndpoint = "spotify-callback"

    private val config = Config.get()
    private val redirectUrl = SpotifyHttpManager.makeUri("http://localhost:$redirectUrlPort/$redirectUrlEndpoint")
    private val scopes = arrayOf(AuthorizationScope.PLAYLIST_READ_PRIVATE)

    private val spotifyApiBuilder = SpotifyApi.Builder()
        .setClientId(config.spotifyClientId)
        .setClientSecret(config.spotifyClientSecret)
        .setRedirectUri(redirectUrl)

    fun getPlaylists(): List<ResultRow> {
        if (DatabaseImpl.spotifyStatus == SpotifyStatus.PLAYLIST_FETCHING) {
            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.NOT_STARTED)
            runBlocking {
                launch(Dispatchers.Default) {
                    authenticate()
                }
            }

            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.PLAYLIST_FETCHING)

            val api = spotifyApiBuilder.setAccessToken(DatabaseImpl.accessToken)
                .setRefreshToken(DatabaseImpl.refreshToken)
                .build()

            var isComplete = false
            var offset = 0

            val allPlaylists = mutableListOf<PlaylistSimplified>()
            while (!isComplete) {
                val playlists = api.listOfCurrentUsersPlaylists.limit(50)
                    .offset(offset)
                    .build()
                    .execute()

                allPlaylists.addAll(playlists.items)

                offset += playlists.items.size
                isComplete = playlists.items.isEmpty()
            }

            transaction {
                allPlaylists.forEachIndexed{ i, playlist ->
                    SpotifyPlaylistTable.insertIgnore {
                        it[playlistId] = playlist.id
                        it[playlistName] = playlist.name
                        it[sortOrder] = (i + 1)
                        it[isIncluded] = false
                    }
                }
            }

            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.PLAYLIST_SELECTION)
        }

        return transaction {
            return@transaction SpotifyPlaylistTable.selectAll().toList()
        }
    }

    fun setPlaylists(playlistsToCheck: List<Int>) {
        transaction {
            SpotifyPlaylistTable.update({ SpotifyPlaylistTable.id inList playlistsToCheck }) {
                it[isIncluded] = true
            }

            SpotifyPlaylistTable.update({ SpotifyPlaylistTable.id notInList playlistsToCheck }) {
                it[isIncluded] = false
            }

            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.ARTIST_FETCHING)
        }
    }

    fun getArtists(): List<ResultRow> {
        if (DatabaseImpl.spotifyStatus == SpotifyStatus.ARTIST_FETCHING) {
            runBlocking {
                launch(Dispatchers.Default) {
                    authenticate()
                }
            }

            val api = spotifyApiBuilder.setAccessToken(DatabaseImpl.accessToken)
                .setRefreshToken(DatabaseImpl.refreshToken)
                .build()

            val allPlaylists = getPlaylists()
            val artistMapping = allPlaylists.filter { it[SpotifyPlaylistTable.isIncluded] }
                .map {
                    var isComplete = false
                    var offset = 0

                    val allPlaylistItems = mutableListOf<PlaylistTrack>()
                    while (!isComplete) {
                        val playlistItems = api.getPlaylistsItems(it[SpotifyPlaylistTable.playlistId])
                            //.fields("items(track(name,href,artists(id,name))),next")
                            .limit(50)
                            .offset(offset)
                            .build()
                            .execute()

                        allPlaylistItems.addAll(playlistItems.items)

                        offset += playlistItems.items.size
                        isComplete = playlistItems.items.isEmpty()
                    }

                    it[SpotifyPlaylistTable.id] to allPlaylistItems.filter { item -> !item.isLocal }
                        .map { item -> item.track as Track }
                        .flatMap { item -> item.artists.toList() }
                        .distinctBy { item -> item.id }
                }

            transaction {
                artistMapping.forEach { (playlistIdColumn, artists) ->
                    artists.forEach { artist ->
                        SpotifyArtistTable.insertIgnore {
                            it[playlistId] = playlistIdColumn
                            it[artistId] = artist.id
                            it[artistName] = artist.name
                            it[isIncluded] = false
                        }
                    }
                }
            }

            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.ARTIST_SELECTION)
        }

        return transaction {
            return@transaction SpotifyArtistTable.selectAll().toList()
        }
    }

    fun setArtists(artistsToCheck: List<Int>) {
        transaction {
            SpotifyArtistTable.update({ SpotifyArtistTable.id inList artistsToCheck }) {
                it[isIncluded] = true
            }

            SpotifyArtistTable.update({ SpotifyPlaylistTable.id notInList artistsToCheck }) {
                it[isIncluded] = false
            }

            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.ALBUM_FETCHING)
        }
    }

    fun getAlbums() {
        TODO("Not yet implemented")
    }

    suspend fun authenticate() {
        if (LocalDateTime.now() < DatabaseImpl.expiresIn) return

        if (!DatabaseImpl.accessToken.isNullOrBlank()) {
            val spotifyApi = spotifyApiBuilder.setRefreshToken(DatabaseImpl.refreshToken).build()

            val authorizationCodeRefresh = spotifyApi.authorizationCodeRefresh().build().executeAsync().await()
            DatabaseImpl.saveSpotifyCredentials(authorizationCodeRefresh)
        } else {
            val spotifyApi = spotifyApiBuilder.build()

            val queries = runOAuthRequest {
                var uri: URI
                runBlocking {
                    uri = spotifyApi.authorizationCodeUri()
                        .scope(*scopes)
                        .build().executeAsync().await()
                }
                return@runOAuthRequest uri
            }

            val code = queries["code"]?.first()
            val authorizationWithCode = spotifyApi.authorizationCode(code).build().executeAsync().await()
            DatabaseImpl.saveSpotifyCredentials(authorizationWithCode)
        }
    }

    private suspend fun runOAuthRequest(uriGenerator: () -> URI): Map<String, List<String>> {
        var responseUrl: URI? = null
        val server = HttpServer.create(InetSocketAddress(redirectUrlPort), 0)
        server.createContext("/$redirectUrlEndpoint") { exchange ->
            responseUrl = exchange.requestURI

            server.stop(0)
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

        return responseUrl.splitQuery()
    }
}