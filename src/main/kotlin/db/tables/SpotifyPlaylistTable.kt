package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object SpotifyPlaylistTable : IntIdTable() {
    val playlistId = text("playlist_id").uniqueIndex()
    val playlistName = text("playlist_name")
    val isIncluded = bool("is_included")
}