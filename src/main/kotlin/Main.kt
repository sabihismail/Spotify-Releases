
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import api.SpotifyImpl
import db.DatabaseImpl
import db.tables.SpotifyPlaylistTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.SpotifyStatus
import org.jetbrains.exposed.sql.ResultRow

enum class Screen {
    LOGIN,
    PLAYLIST_SELECTION,
}

@Composable
@Preview
fun LoginView(onComplete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = {
            SpotifyImpl.getPlaylists()

            if (DatabaseImpl.spotifyStatus == SpotifyStatus.PLAYLIST_SELECTION) {
                onComplete()
            }
        }) {
            Text("Login to Spotify")
        }
    }
}

@Composable
@Preview
fun PlaylistView(playlists: List<ResultRow>) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val playlistsToCheck by remember {
            mutableStateOf(playlists.filter { it[SpotifyPlaylistTable.isIncluded] }.map { it[SpotifyPlaylistTable.id].value }.toMutableList())
        }

        Column(horizontalAlignment = Alignment.End) {
            LazyColumn {
                items(playlists) { playlist ->
                    val playlistDbId = playlist[SpotifyPlaylistTable.id].value
                    val isIncludedInResults = playlist[SpotifyPlaylistTable.isIncluded]

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(isIncludedInResults, onCheckedChange = { checked ->
                            if (checked) {
                                playlistsToCheck.remove(playlistDbId)
                            } else {
                                playlistsToCheck.add(playlistDbId)
                            }
                        })

                        Text(playlist[SpotifyPlaylistTable.playlistName])
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
}

@Composable
fun MainView() {
    var screenState by remember {
        mutableStateOf(
            when (DatabaseImpl.spotifyStatus) {
                SpotifyStatus.NOT_STARTED, SpotifyStatus.AUTHENTICATED, SpotifyStatus.PLAYLIST_FETCHING -> Screen.LOGIN
                SpotifyStatus.PLAYLIST_SELECTION -> Screen.PLAYLIST_SELECTION
                else -> { Screen.LOGIN }
            }
        )
    }

    when (screenState) {
        Screen.LOGIN -> LoginView(onComplete = { screenState = Screen.PLAYLIST_SELECTION })
        Screen.PLAYLIST_SELECTION -> PlaylistView(SpotifyImpl.getPlaylists())
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            MainView()
        }
    }
}
