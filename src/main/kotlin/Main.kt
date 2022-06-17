
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import api.SpotifyImpl
import db.DatabaseImpl
import db.models.GenericKeyValueKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.SpotifyStatus
import java.util.concurrent.atomic.AtomicReference

val status = AtomicReference<SpotifyStatus>()

@Composable
@Preview
fun LoginView() {
    MaterialTheme {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = {
                runBlocking {
                    launch(Dispatchers.Default) {
                        SpotifyImpl.getPlaylists()
                    }
                }
            }) {
                Text("Login to Spotify")
            }
        }
    }
}

@Composable
@Preview
fun PlaylistView() {
    MaterialTheme {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = {
                runBlocking {
                    launch(Dispatchers.Default) {
                        SpotifyImpl.getPlaylists()
                    }
                }
            }) {
                Text("Login to Spotify")
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        when (DatabaseImpl.getValue<SpotifyStatus>(GenericKeyValueKey.SPOTIFY_STATUS)) {
            SpotifyStatus.NOT_STARTED -> LoginView()
            SpotifyStatus.PLAYLIST_FETCHING -> LoginView()
            SpotifyStatus.PLAYLIST_SELECTION -> PlaylistView()
            else -> {}
        }
    }
}
