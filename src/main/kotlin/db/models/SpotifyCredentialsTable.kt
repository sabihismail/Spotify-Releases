package db.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object SpotifyCredentialsTable : IntIdTable() {
    val accessToken = text("accessToken")
    val refreshToken = text("refreshToken")
    val expiresIn = datetime("expiresIn")
}