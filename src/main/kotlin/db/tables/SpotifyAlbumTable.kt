package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date

object SpotifyAlbumTable: IntIdTable() {
    val artistId = reference("artist_id", SpotifyArtistTable.id)
    val albumId = text("album_id")
    val albumName = text("album_name")
    val albumDate = date("album_date")
    val albumImageUrl = text("album_image_url")
}