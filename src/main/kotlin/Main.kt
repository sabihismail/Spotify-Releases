
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import views.MainView


fun main() = application {
    Window(title = "Spotify Releases", onCloseRequest = ::exitApplication) {
        MaterialTheme {
            MainView()
        }
    }
}
