package db

import db.models.SpotifyCredentialsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials
import java.sql.Connection
import java.time.LocalDateTime

object DatabaseImpl {
    init {
        Database.connect("jdbc:sqlite:database.db", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                SpotifyCredentialsTable,
            )
        }
    }

    val accessToken: String? get() {
        var str: String? = null
        transaction {
            str = SpotifyCredentialsTable.selectAll().firstOrNull()?.getOrNull(SpotifyCredentialsTable.accessToken)
        }
        return str
    }

    val refreshToken: String? get() {
        var str: String? = null
        transaction {
            str = SpotifyCredentialsTable.selectAll().firstOrNull()?.getOrNull(SpotifyCredentialsTable.refreshToken)
        }
        return str
    }

    val expiresIn: LocalDateTime? get() {
        return transaction {
            SpotifyCredentialsTable.selectAll().firstOrNull()?.getOrNull(SpotifyCredentialsTable.expiresIn) ?: LocalDateTime.MIN
        }
    }

    fun saveSpotifyCredentials(authorizationCodeCredentials: AuthorizationCodeCredentials?) {
        if (authorizationCodeCredentials == null) return

        transaction {
            SpotifyCredentialsTable.deleteAll()
            SpotifyCredentialsTable.insert {
                it[accessToken] = authorizationCodeCredentials.accessToken
                it[refreshToken] = authorizationCodeCredentials.refreshToken
                it[expiresIn] = LocalDateTime.now().plusSeconds(authorizationCodeCredentials.expiresIn.toLong())
            }
        }
    }
}