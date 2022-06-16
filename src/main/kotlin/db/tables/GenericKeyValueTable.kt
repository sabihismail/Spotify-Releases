package db.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object GenericKeyValueTable: IntIdTable() {
    val key = text("key").uniqueIndex()
    val value = text("value")
}