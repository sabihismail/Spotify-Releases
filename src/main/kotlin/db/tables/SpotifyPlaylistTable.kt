package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object SpotifyPlaylistTable : IntIdTable() {
    val playlistId = text("playlistId").uniqueIndex()
    val playlistName = text("playlistName")
    val isIncluded = bool("isIncluded")
}