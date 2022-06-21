package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import org.jetbrains.exposed.sql.ResultRow

@Composable
@Preview
fun AlbumView(albums: List<ResultRow>) {

}