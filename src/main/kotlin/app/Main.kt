package app

import app.di.ModuleLoader.configureDI
import infrastructure.persistence.DatabaseFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.java.KoinJavaComponent.get
import presentation.TelegramBot.runTelegramBot

val logger = KotlinLogging.logger {}

suspend fun main() {
    logger.info { "" }
    logger.info { "\t.__  .__ __                                                 __  .__\t\t" }
    logger.info { "\t|  | |__|  | __ ____   ___________________  ____      _____/  |_|  |__\t" }
    logger.info { "\t|  | |  |  |/ // __ \\ /  ___/\\____ \\_  __ \\/  _ \\   _/ __ \\   __\\  |  \\\t" }
    logger.info { "\t|  |_|  |    <\\  ___/ \\___ \\ |  |_> >  | \\(  <_> )  \\  ___/|  | |   Y  \\" }
    logger.info { "\t|____/__|__|_ \\\\___  >____  >|   __/|__|   \\____/ /\\ \\___  >__| |___|  /" }
    logger.info { "\t             \\/    \\/     \\/ |__|                 \\/     \\/          \\/\t" }
    logger.info { "\t                                                                            " }
    logger.info { "" }
    logger.info { "GitHub: @likespro | X (Twitter): @likespro_eth | E-Mail: likespro.eth@gmail.com" }
    logger.info { "" }

    configureDI()
    get<DatabaseFactory>(DatabaseFactory::class.java).init()
    runTelegramBot()
}