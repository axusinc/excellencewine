package presentation

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import presentation.admin.addHeadsOfExpertsFlow

object TelegramBot {
    suspend fun runTelegramBot() {
        val bot = telegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

        bot.buildBehaviourWithLongPolling {
            println(getMe())

            startBotFlow()
            addHeadsOfExpertsFlow()
        }.join()
    }
}