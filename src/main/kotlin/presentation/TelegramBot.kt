package presentation

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import presentation.InlineMarkupPaginationUtils.setupInlineMarkupPagination
import presentation.StartBotFlow.setupStartBotFlow
import presentation.flows.AddUserFlow.setupAddUserFlow
import presentation.flows.AssessFlow.setupAssessFlow
import presentation.flows.BackFlow.setupBackButtonFlow
import presentation.flows.CreateCompetitionFlow.setupCreateCompetitionFlow
import presentation.flows.EndCompetitionFlow.setupEndCompetitionFlow
import presentation.flows.GetCompetitionResultsFlow.setupGetCompetitionResultsFlow
import presentation.flows.MyMarksFlow.setupMyMarksFlow
import presentation.flows.PreviewResultsFlow.setupPreviewResultsFlow

object TelegramBot {
    suspend fun runTelegramBot() {
        val bot = telegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

        bot.buildBehaviourWithLongPolling {
            println(getMe())

            setupInlineMarkupPagination()
            setupStartBotFlow()
            setupBackButtonFlow()
            setupAddUserFlow()
            setupGetCompetitionResultsFlow()
            setupCreateCompetitionFlow()
            setupAssessFlow()
            setupMyMarksFlow()
            setupPreviewResultsFlow()
            setupEndCompetitionFlow()
        }.join()
    }
}