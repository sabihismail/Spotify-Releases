package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import api.SpotifyImpl
import db.tables.SpotifyPlaylistTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.ResultRow
import views.enums.CurrentView
import views.util.LabelledCheckBox

@Composable
@Preview
fun PlaylistView(playlists: List<ResultRow>, changeView: (CurrentView) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val playlistsToCheck by remember {
            mutableStateOf(playlists.filter { it[SpotifyPlaylistTable.isIncluded] }.map { it[SpotifyPlaylistTable.id].value }.toMutableList())
        }

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            items(playlists) { playlist ->
                val playlistDbId = playlist[SpotifyPlaylistTable.id].value
                var isIncludedInResults by remember { mutableStateOf(playlist[SpotifyPlaylistTable.isIncluded]) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabelledCheckBox(isIncludedInResults, label = playlist[SpotifyPlaylistTable.playlistName], onCheckedChange = { checked ->
                        if (checked) {
                            playlistsToCheck.remove(playlistDbId)
                        } else {
                            playlistsToCheck.add(playlistDbId)
                        }

                        isIncludedInResults = checked
                    })
                }
            }
        }

        Button(onClick = {
            runBlocking {
                launch(Dispatchers.Default) {
                    SpotifyImpl.setPlaylists(playlistsToCheck.toList())
                }
            }
        }) {
            Text("Save")
        }
    }
}
