package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import api.SpotifyImpl
import db.DatabaseImpl
import db.tables.SpotifyArtistTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.enums.SpotifyStatus
import views.enums.CurrentView

@Composable
@Preview
fun ArtistView(changeView: (CurrentView) -> Unit) {
    CheckboxView(
        ButtonRow = { checkedEntries ->
            Button(onClick = {
                runBlocking {
                    launch(Dispatchers.Default) {
                        SpotifyImpl.setArtists(checkedEntries.toList())
                        SpotifyImpl.getAlbums()

                        if (DatabaseImpl.spotifyStatus == SpotifyStatus.ARTIST_SELECTION) {
                            changeView(CurrentView.ALBUM_VIEW)
                        }
                    }
                }
            }) {
                Text("Save")
            }
        },
        entries = SpotifyImpl.getArtists(),
        idColumn = SpotifyArtistTable.id,
        checkedColumn = SpotifyArtistTable.isIncluded,
        labelColumn = SpotifyArtistTable.artistName,
    )
}
