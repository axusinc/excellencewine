package infrastructure.persistence

import org.jetbrains.exposed.sql.Database

class DatabaseFactory(
    private val url: String,
    private val user: String,
    private val password: String
) {
    fun init() {
        Database.connect(
            url = url,
            user = user,
            password = password
        )
    }
}