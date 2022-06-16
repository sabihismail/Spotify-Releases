
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import api.SpotifyImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
@Preview
fun App() {
    MaterialTheme {
        Button(onClick = {
            runBlocking {
                launch(Dispatchers.Default) {
                    SpotifyImpl.get()
                }
            }
        }) {
            Text("Login to Spotify")
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
