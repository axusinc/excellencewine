package infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

class DatabaseFactory(
    private val url: String,
    private val user: String,
    private val password: String
) {
    fun init() {
//        val config = HikariConfig().apply {
//            jdbcUrl = url
//            driverClassName = "org.postgresql.Driver"
//            username = user
//            password = this@DatabaseFactory.password
//            maximumPoolSize = 5
//            isAutoCommit = true
//            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
//            validate()
//        }
//        val dataSource = HikariDataSource(config)
//        Database.connect(dataSource)

        Database.connect(
            url = url,
            user = user,
            password = password,
        )
    }
}