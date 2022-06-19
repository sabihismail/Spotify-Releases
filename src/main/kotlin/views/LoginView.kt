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
import models.SpotifyStatus
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
            SpotifyImpl.getPlaylists()

            if (DatabaseImpl.spotifyStatus == SpotifyStatus.PLAYLIST_SELECTION) {
                changeView(CurrentView.PLAYLIST_SELECTION)
            }
        }) {
            Text("Login to Spotify")
        }
    }
}