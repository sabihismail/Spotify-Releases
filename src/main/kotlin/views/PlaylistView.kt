package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import api.SpotifyImpl
import db.DatabaseImpl
import db.tables.SpotifyPlaylistTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.enums.SpotifyStatus
import views.enums.CurrentView

@Composable
@Preview
fun PlaylistView(changeView: (CurrentView) -> Unit) {
    CheckboxView(
        ButtonRow = { checkedCheckboxIds ->
            Button(onClick = {
                runBlocking {
                    launch(Dispatchers.Default) {
                        SpotifyImpl.setPlaylists(checkedCheckboxIds.toList())
                        SpotifyImpl.getArtists()

                        if (DatabaseImpl.spotifyStatus == SpotifyStatus.PLAYLIST_SELECTION) {
                            changeView(CurrentView.ARTIST_SELECTION)
                        }
                    }
                }
            }) {
                Text("Save")
            }
        },
        entries = SpotifyImpl.getPlaylists(),
        idColumn = SpotifyPlaylistTable.id,
        checkedColumn = SpotifyPlaylistTable.isIncluded,
        labelColumn = SpotifyPlaylistTable.playlistName,
    )
}