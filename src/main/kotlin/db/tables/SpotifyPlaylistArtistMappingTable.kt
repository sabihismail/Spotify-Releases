package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object SpotifyPlaylistArtistMappingTable: IntIdTable() {
    val playlistId = reference("playlist_id", SpotifyPlaylistTable.id)
    val artistId = reference("artist_id", SpotifyArtistTable.id)
}