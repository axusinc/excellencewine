package presentation.flows

import app.logger
import application.usecase.GetActiveCompetitionRequest
import application.usecase.GetUserByChatIdRequest
import application.usecase.UpdateConversationStateRequest
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import domain.model.entity.User
import domain.model.value.ConversationState
import domain.ports.repositories.CompetitionRepository
import domain.ports.repositories.VineAssessmentRepository
import eth.likespro.commons.models.value.Timestamp
import infrastructure.persistence.tables.UsersTable
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings
import presentation.FlowUtils.sendIncorrectStateMessage
import presentation.MenuUtils
import presentation.ReportUtils.generateReport
import presentation.RetryUtils.tryWithRetry

object EndCompetitionFlow {
    suspend fun <BC : BehaviourContext> BC.setupEndCompetitionFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.END_COMPETITION))) { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.HEAD_OF_EXPERTS)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Керівникам експертів.")
                return@onText
            }

            reply(message, "Ви обрали завершити конкурс.", replyMarkup = MenuUtils.BACK_BUTTON_MENU)
            UpdateConversationStateRequest(
                user.chatId!!,
                ConversationState.END_COMPETITION_REQUESTED
            ).execute()

            val activeCompetition = GetActiveCompetitionRequest().execute()!!
            reply(message, "Ви впевнені, що хочете завершити конкурс ${activeCompetition.name.value}?", replyMarkup = InlineKeyboardMarkup(matrix {
                row {
                    +CallbackDataInlineKeyboardButton(CommonStrings.YES, "competition_end_yes")
                    +CallbackDataInlineKeyboardButton(CommonStrings.NO, "competition_end_no")
                }
            }))
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery("competition_end_yes") { query -> try {
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.END_COMPETITION_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            endCompetition(user)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery("competition_end_no") { query -> try {
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.END_COMPETITION_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            UpdateConversationStateRequest(
                user.chatId!!,
                ConversationState.INITIAL
            ).execute()

            send(query.user.id, "Завершення конкурсу скасовано.", replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, true))
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }
    }





    suspend fun <BC : BehaviourContext> BC.endCompetition(user: User) {
        val activeCompetition = GetActiveCompetitionRequest().execute()!!
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.INITIAL
        ).execute()

        val vineAssessmentRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
        val assessments = vineAssessmentRepository.filter(competitionId = activeCompetition.id)
        val reportFile = generateReport(activeCompetition, assessments, "report-")

        val competitionRepository: CompetitionRepository by inject(CompetitionRepository::class.java)
        competitionRepository.update(activeCompetition.apply { endedAt = Timestamp.now() })!!

        send(user.chatId.toChatId(), "Конкурс ${activeCompetition.name.value} успішно завершено.")

        newSuspendedTransaction {
            UsersTable.selectAll().forEach { row ->
                try {
                    tryWithRetry(3, 1000) {
                        val rowUser = UsersTable.fromRow(row)
                        if(rowUser.chatId != null) {
                            UpdateConversationStateRequest(rowUser.chatId, ConversationState.INITIAL).execute()
                            sendDocument(rowUser.chatId.toChatId(), InputFile.fromFile(reportFile), text = "Конкурс ${activeCompetition.name.value} завершено. Таблиця з результатами конкурсу прикріплена нижче.", replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, rowUser.role, false))
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to send competition started message to user $row" }
                }
                delay(500)
            }
        }
        reportFile.delete()
    }
}