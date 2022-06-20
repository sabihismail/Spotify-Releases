package views

import androidx.compose.runtime.*
import api.SpotifyImpl
import db.DatabaseImpl
import db.tables.SpotifyArtistTable
import db.tables.SpotifyPlaylistTable
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
        CurrentView.LOGIN -> LoginView(changeView = changeView)
        CurrentView.PLAYLIST_SELECTION -> CheckboxView(
            SpotifyImpl.getPlaylists(),
            idColumn = SpotifyPlaylistTable.id,
            checkedColumn = SpotifyPlaylistTable.isIncluded,
            labelColumn = SpotifyPlaylistTable.playlistName,
            complete = { checkedEntries ->
                SpotifyImpl.setPlaylists(checkedEntries.toList())
                SpotifyImpl.getArtists()

                if (DatabaseImpl.spotifyStatus == SpotifyStatus.PLAYLIST_SELECTION) {
                    changeView(CurrentView.ARTIST_SELECTION)
                }
            },
            changeView = changeView
        )
        CurrentView.ARTIST_SELECTION -> CheckboxView(
            SpotifyImpl.getArtists().sortedBy { it[SpotifyArtistTable.artistName] },
            idColumn = SpotifyArtistTable.id,
            checkedColumn = SpotifyArtistTable.isIncluded,
            labelColumn = SpotifyArtistTable.artistName,
            complete = { checkedEntries ->
                SpotifyImpl.setArtists(checkedEntries.toList())
                SpotifyImpl.getAlbums()

                if (DatabaseImpl.spotifyStatus == SpotifyStatus.ARTIST_SELECTION) {
                    changeView(CurrentView.ALBUM_VIEW)
                }
            },
            changeView = changeView
        )
        CurrentView.ALBUM_VIEW -> LoginView(changeView)
    }
}
