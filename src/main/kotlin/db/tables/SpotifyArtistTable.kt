package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object SpotifyArtistTable: IntIdTable() {
    val artistId = text("artist_id").uniqueIndex()
    val artistName = text("artist_name")
    val isIncluded = bool("is_included")
}