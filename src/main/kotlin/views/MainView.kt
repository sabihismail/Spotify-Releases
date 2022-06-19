package views

import androidx.compose.runtime.*
import api.SpotifyImpl
import db.DatabaseImpl
import models.SpotifyStatus
import views.enums.CurrentView


@Composable
fun MainView() {
    var screenState by remember {
        mutableStateOf(
            when (DatabaseImpl.spotifyStatus) {
                SpotifyStatus.NOT_STARTED, SpotifyStatus.AUTHENTICATED, SpotifyStatus.PLAYLIST_FETCHING -> CurrentView.LOGIN
                SpotifyStatus.PLAYLIST_SELECTION -> CurrentView.PLAYLIST_SELECTION
                else -> {
                    CurrentView.LOGIN
                }
            }
        )
    }

    val changeView = { nextScreen: CurrentView -> screenState = nextScreen }
    when (screenState) {
        CurrentView.LOGIN -> LoginView(changeView = changeView)
        CurrentView.PLAYLIST_SELECTION -> PlaylistView(SpotifyImpl.getPlaylists(), changeView = changeView)
    }
}
