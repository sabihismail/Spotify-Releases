package views

import androidx.compose.runtime.*
import api.SpotifyImpl
import db.DatabaseImpl
import models.enums.SpotifyStatus
import views.enums.CurrentView


@Composable
fun MainView() {
    var screenState by remember {
        mutableStateOf(
            when (DatabaseImpl.spotifyStatus) {
                SpotifyStatus.NOT_STARTED, SpotifyStatus.AUTHENTICATED, SpotifyStatus.PLAYLIST_FETCHING -> CurrentView.LOGIN
                SpotifyStatus.PLAYLIST_SELECTION -> CurrentView.PLAYLIST_SELECTION
                SpotifyStatus.ARTIST_SELECTION -> CurrentView.ARTIST_SELECTION
                else -> CurrentView.LOGIN
            }
        )
    }

    val changeView = { nextScreen: CurrentView -> screenState = nextScreen }
    when (screenState) {
        CurrentView.LOGIN -> LoginView(changeView)
        CurrentView.PLAYLIST_SELECTION -> PlaylistView(changeView)
        CurrentView.ARTIST_SELECTION -> ArtistView(changeView)
        CurrentView.ALBUM_VIEW -> AlbumView(SpotifyImpl.getAlbums())
    }
}
