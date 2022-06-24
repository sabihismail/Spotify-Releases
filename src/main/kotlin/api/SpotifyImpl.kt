package api

import com.sun.net.httpserver.HttpServer
import db.DatabaseImpl
import db.models.GenericKeyValueKey
import db.tables.SpotifyAlbumTable
import db.tables.SpotifyArtistTable
import db.tables.SpotifyPlaylistArtistMappingTable
import db.tables.SpotifyPlaylistTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import models.enums.SpotifyStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.SpotifyHttpManager
import se.michaelthelin.spotify.enums.AuthorizationScope
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.model_objects.specification.Track
import util.Config
import util.extensions.UrlExtensions.splitQuery
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

object SpotifyImpl {
    private const val redirectUrlPort = 9229
    private const val redirectUrlEndpoint = "spotify-callback"

    private val config = Config.get()
    private val redirectUrl = SpotifyHttpManager.makeUri("http://localhost:$redirectUrlPort/$redirectUrlEndpoint")
    private val scopes = arrayOf(AuthorizationScope.PLAYLIST_READ_PRIVATE, AuthorizationScope.USER_TOP_READ)

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
        runBlocking {
            launch(Dispatchers.Default) {
                authenticate()
            }
        }

        val api = spotifyApiBuilder.setAccessToken(DatabaseImpl.accessToken)
            .setRefreshToken(DatabaseImpl.refreshToken)
            .build()

        if (DatabaseImpl.spotifyStatus == SpotifyStatus.ARTIST_FETCHING) {
            val allPlaylists = getPlaylists()
            val artistMapping = allPlaylists.map {
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
                            it[artistId] = artist.id
                            it[artistName] = artist.name
                            it[isIncluded] = false
                        }

                        val artistIdSaved = SpotifyArtistTable.slice(SpotifyArtistTable.id)
                            .select { SpotifyArtistTable.artistId eq artist.id }
                            .first()[SpotifyArtistTable.id]

                        SpotifyPlaylistArtistMappingTable.insertIgnore {
                            it[playlistId] = playlistIdColumn
                            it[artistId] = artistIdSaved
                        }
                    }
                }
            }

            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.ARTIST_SELECTION)
        }

        val topArtists = api.usersTopArtists.time_range("long_term")
            .limit(50)
            .build()
            .execute()
            .items
            .mapIndexed { i, artist -> artist.id to i }
            .toMap()

        return transaction {
            return@transaction (SpotifyArtistTable innerJoin SpotifyPlaylistArtistMappingTable innerJoin SpotifyPlaylistTable)
                .slice(SpotifyArtistTable.columns)
                .select { SpotifyPlaylistTable.isIncluded eq true }
                .distinctBy { it[SpotifyArtistTable.id] }
                .sortedWith(
                    compareBy<ResultRow> { artist -> topArtists.getOrDefault(artist[SpotifyArtistTable.artistId], Int.MAX_VALUE) }
                        .thenBy { artist -> artist[SpotifyArtistTable.artistName] }
                )
                .toList()
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

    fun getAlbums(): List<ResultRow> {
        if (DatabaseImpl.spotifyStatus == SpotifyStatus.ALBUM_FETCHING) {
            runBlocking {
                launch(Dispatchers.Default) {
                    authenticate()
                }
            }

            val api = spotifyApiBuilder.setAccessToken(DatabaseImpl.accessToken)
                .setRefreshToken(DatabaseImpl.refreshToken)
                .build()

            val allArtists = getArtists()
            val albumMapping = allArtists
                .map {
                    var isComplete = false
                    var offset = 0

                    val allAlbums = mutableListOf<AlbumSimplified>()
                    while (!isComplete) {
                        val albums = api.getArtistsAlbums(it[SpotifyArtistTable.artistId])
                            .limit(50)
                            .offset(offset)
                            .build()
                            .execute()

                        allAlbums.addAll(albums.items)

                        offset += albums.items.size
                        isComplete = albums.items.isEmpty()
                    }

                    it[SpotifyArtistTable.id] to allAlbums
                }

            transaction {
                albumMapping.forEach { (artistIdColumn, albums) ->
                    albums.forEach { album ->
                        SpotifyAlbumTable.insertIgnore {
                            it[artistId] = artistIdColumn
                            it[albumId] = album.id
                            it[albumName] = album.name
                            it[albumDate] = LocalDate.parse(album.releaseDate)
                            it[albumImageUrl] = album.images.sortedByDescending { image -> image.height }.first().url
                        }
                    }
                }
            }

            DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.COMPLETED)
        }

        return transaction {
            return@transaction (SpotifyAlbumTable innerJoin SpotifyArtistTable innerJoin SpotifyPlaylistTable)
                .slice(listOf(SpotifyAlbumTable.columns, SpotifyArtistTable.columns).flatten())
                .select { SpotifyPlaylistTable.isIncluded and SpotifyArtistTable.isIncluded }
                .toList()
        }
    }

    suspend fun authenticate() {
        if (LocalDateTime.now() < DatabaseImpl.expiresIn) return

        val scopesStr = scopes.joinToString(", ")
        if (DatabaseImpl.scopes == scopesStr && !DatabaseImpl.accessToken.isNullOrBlank()) {
            val spotifyApi = spotifyApiBuilder.setRefreshToken(DatabaseImpl.refreshToken).build()

            val authorizationCodeRefresh = spotifyApi.authorizationCodeRefresh().build().executeAsync().await()
            DatabaseImpl.saveSpotifyCredentials(authorizationCodeRefresh, scopesStr)
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
            DatabaseImpl.saveSpotifyCredentials(authorizationWithCode, scopesStr)
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