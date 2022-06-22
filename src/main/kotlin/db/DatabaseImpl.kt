package db

import db.models.GenericKeyValueKey
import db.tables.GenericKeyValueTable
import db.tables.SpotifyArtistTable
import db.tables.SpotifyPlaylistArtistMappingTable
import db.tables.SpotifyPlaylistTable
import models.enums.SpotifyStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials
import util.extensions.DateTimeExtensions.toEpochSecond
import util.extensions.DateTimeExtensions.toLocalDateTime
import java.sql.Connection
import java.time.LocalDateTime

object DatabaseImpl {
    init {
        Database.connect("jdbc:sqlite:database.db", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                GenericKeyValueTable,
                SpotifyPlaylistTable,
                SpotifyArtistTable,
                SpotifyPlaylistArtistMappingTable,
            )
        }
    }

    val accessToken get() = getValue<String>(GenericKeyValueKey.SPOTIFY_API_ACCESS_TOKEN)
    val refreshToken get() = getValue<String>(GenericKeyValueKey.SPOTIFY_API_REFRESH_TOKEN)
    val expiresIn: LocalDateTime get() = getValue<LocalDateTime>(GenericKeyValueKey.SPOTIFY_API_EXPIRES_IN) ?: LocalDateTime.MIN
    val scopes get() = getValue<String>(GenericKeyValueKey.SPOTIFY_API_SCOPES)
    val spotifyStatus get() = getValue<SpotifyStatus>(GenericKeyValueKey.SPOTIFY_STATUS) ?: SpotifyStatus.NOT_STARTED

    fun saveSpotifyCredentials(authorizationCodeCredentials: AuthorizationCodeCredentials?, scopesStr: String) {
        if (authorizationCodeCredentials == null) return

        setValue(GenericKeyValueKey.SPOTIFY_API_ACCESS_TOKEN, authorizationCodeCredentials.accessToken)
        setValue(GenericKeyValueKey.SPOTIFY_API_EXPIRES_IN, LocalDateTime.now().plusSeconds(authorizationCodeCredentials.expiresIn.toLong()))
        setValue(GenericKeyValueKey.SPOTIFY_API_SCOPES, scopesStr)

        if (!authorizationCodeCredentials.refreshToken.isNullOrBlank()) {
            setValue(GenericKeyValueKey.SPOTIFY_API_REFRESH_TOKEN, authorizationCodeCredentials.refreshToken)
        }
    }

    inline fun <reified T> getValue(key: GenericKeyValueKey): T? {
        val keyId = key.name

        return transaction {
            val result = GenericKeyValueTable.select { GenericKeyValueTable.key eq keyId }.singleOrNull() ?: return@transaction null
            val returned = result[GenericKeyValueTable.value]

            return@transaction when (T::class) {
                String::class -> returned as T
                LocalDateTime::class -> returned.toLongOrNull().toLocalDateTime() as T
                SpotifyStatus::class -> enumValueOf<SpotifyStatus>(returned) as T
                else -> null
            }
        }
    }

    fun <T : Enum<T>> setValue(keyIn: GenericKeyValueKey, valueIn: Enum<T>) = setValue(keyIn, valueIn.toString())
    fun setValue(keyIn: GenericKeyValueKey, valueIn: LocalDateTime) = setValue(keyIn, valueIn.toEpochSecond().toString())
    fun setValue(keyIn: GenericKeyValueKey, valueIn: String) {
        val keyId = keyIn.name
        transaction {
            val query: (SqlExpressionBuilder.() -> Op<Boolean>) = { GenericKeyValueTable.key eq keyId }
            if (GenericKeyValueTable.select(query).count() == 1L) {
                GenericKeyValueTable.update(query) { update ->
                    update[value] = valueIn
                }
            } else {
                GenericKeyValueTable.insert { insert ->
                    insert[key] = keyId
                    insert[value] = valueIn
                }
            }
        }
    }
}

