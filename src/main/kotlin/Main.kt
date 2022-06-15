import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import api.SpotifyImpl

@Composable
@Preview
fun App() {
    MaterialTheme {
        Button(onClick = {
            SpotifyImpl.login()
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
