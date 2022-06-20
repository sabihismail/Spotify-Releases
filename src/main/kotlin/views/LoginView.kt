package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import api.SpotifyImpl
import db.DatabaseImpl
import db.models.GenericKeyValueKey
import db.tables.SpotifyArtistTable
import db.tables.SpotifyPlaylistTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import models.enums.SpotifyStatus
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import views.enums.CurrentView


@Composable
@Preview
fun LoginView(changeView: (CurrentView) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = {
            runBlocking {
                withContext(Dispatchers.Default) {
                    SpotifyImpl.authenticate()
                }
            }

            if (transaction { return@transaction SpotifyPlaylistTable.selectAll().count() } == 0L) {
                DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.PLAYLIST_FETCHING)
                SpotifyImpl.getPlaylists()

                changeView(CurrentView.PLAYLIST_SELECTION)
                return@Button
            } else if (transaction { return@transaction SpotifyPlaylistTable.selectAll().count { it[SpotifyPlaylistTable.isIncluded] } } == 0) {
                DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.PLAYLIST_SELECTION)
                changeView(CurrentView.PLAYLIST_SELECTION)
            } else if (transaction { return@transaction SpotifyArtistTable.selectAll().count() } == 0L) {
                DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.ARTIST_FETCHING)
                SpotifyImpl.getArtists()

                changeView(CurrentView.ARTIST_SELECTION)
            } else if (transaction { return@transaction SpotifyArtistTable.selectAll().count { it[SpotifyArtistTable.isIncluded] } } == 0) {
                DatabaseImpl.setValue(GenericKeyValueKey.SPOTIFY_STATUS, SpotifyStatus.ARTIST_SELECTION)
                changeView(CurrentView.ARTIST_SELECTION)
            }
        }) {
            Text("Login to Spotify")
        }
    }
}