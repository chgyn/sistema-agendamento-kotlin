package com.barbershop.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private lateinit var dataSource: DataSource

    fun init(config: ApplicationConfig) {
        val url = config.propertyOrNull("database.url")?.getString()
            ?: "jdbc:postgresql://localhost:5432/barbershop_db"
        val user = config.propertyOrNull("database.user")?.getString() ?: "barber_user"
        val password = config.propertyOrNull("database.password")?.getString() ?: "barber_pass123"
        val maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull() ?: 10

        logger.info("Inicializando conexão com banco de dados em: {}", url)

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            poolName = "BarbershopHikariPool"
            validate()
        }

        dataSource = HikariDataSource(hikariConfig)

        runFlyway(dataSource)
        Database.connect(dataSource)
        logger.info("Exposed ORM conectado com sucesso via HikariCP.")
    }

    fun initForTests(testDataSource: DataSource) {
        dataSource = testDataSource
        runFlyway(dataSource)
        Database.connect(dataSource)
    }

    private fun runFlyway(dataSource: DataSource) {
        logger.info("Executando migrações Flyway...")
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
        flyway.migrate()
        logger.info("Migrações Flyway executadas com sucesso.")
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
