package api

import se.michaelthelin.spotify.SpotifyApi
import util.Config

object SpotifyImpl {
    private val config = Config.get()
    private val spotifyApi = SpotifyApi.Builder().setClientId(config.spotifyClientId)
        .setClientSecret(config.spotifyClientSecret)
        .build()

    fun get() {
    }

    fun login() {
        val credentials = spotifyApi.clientCredentials().build().executeAsync()

    }
}