package db

import db.models.GenericKeyValueKey
import db.tables.GenericKeyValueTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials
import util.DateTimeExtension.toEpochSecond
import util.DateTimeExtension.toLocalDateTime
import java.sql.Connection
import java.time.LocalDateTime

object DatabaseImpl {
    init {
        Database.connect("jdbc:sqlite:database.db", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                GenericKeyValueTable,
            )
        }
    }

    val accessToken get() = getValue(GenericKeyValueKey.SPOTIFY_ACCESS_TOKEN)
    val refreshToken get() = getValue(GenericKeyValueKey.SPOTIFY_REFRESH_TOKEN)
    val expiresIn get() = getValue(GenericKeyValueKey.SPOTIFY_EXPIRES_IN)?.toLongOrNull().toLocalDateTime()

    fun saveSpotifyCredentials(authorizationCodeCredentials: AuthorizationCodeCredentials?) {
        if (authorizationCodeCredentials == null) return

        setValue(GenericKeyValueKey.SPOTIFY_ACCESS_TOKEN, authorizationCodeCredentials.accessToken)
        setValue(GenericKeyValueKey.SPOTIFY_REFRESH_TOKEN, authorizationCodeCredentials.accessToken)
        setValue(GenericKeyValueKey.SPOTIFY_EXPIRES_IN, LocalDateTime.now().plusSeconds(authorizationCodeCredentials.expiresIn.toLong()).toEpochSecond())
    }

    fun getValue(key: GenericKeyValueKey): String? {
        var str: String? = null

        val keyId = key.name
        transaction {
            val result = GenericKeyValueTable.select { GenericKeyValueTable.key eq keyId }.singleOrNull() ?: return@transaction
            str = result[GenericKeyValueTable.value]
        }

        return str
    }

    fun setValue(keyIn: GenericKeyValueKey, valueIn: Any) {
        val keyId = keyIn.name
        transaction {
            val query: (SqlExpressionBuilder.() -> Op<Boolean>) = { GenericKeyValueTable.key eq keyId }
            if (GenericKeyValueTable.select(query).count() == 1L) {
                GenericKeyValueTable.update(query) { update ->
                    update[value] = valueIn.toString()
                }
            } else {
                GenericKeyValueTable.insert { insert ->
                    insert[key] = keyId
                    insert[value] = valueIn.toString()
                }
            }
        }
    }
}

