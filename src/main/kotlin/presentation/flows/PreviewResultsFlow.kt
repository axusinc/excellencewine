package presentation.flows

import application.usecase.GetActiveCompetitionRequest
import application.usecase.GetUserByChatIdRequest
import application.usecase.UpdateConversationStateRequest
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.toChatId
import domain.model.entity.User
import domain.model.value.ConversationState
import domain.ports.repositories.VineAssessmentRepository
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings
import presentation.MenuUtils
import presentation.ReportUtils.generateReport

object PreviewResultsFlow {
    suspend fun <BC : BehaviourContext> BC.setupPreviewResultsFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.PREVIEW_RESULTS))) { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.HEAD_OF_EXPERTS)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Керівникам експертів.")
                return@onText
            }

            reply(message, "Таблиця з оцінками готується...")

            sendResults(user, message)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }
    }

    suspend fun <BC : BehaviourContext> BC.sendResults(user: User, message: AccessibleMessage) {
        val activeCompetition = runBlocking { GetActiveCompetitionRequest().execute()!! }
        val vineAssessmentRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
        val assessments = vineAssessmentRepository.filter(competitionId = activeCompetition.id)

        val report = generateReport(activeCompetition, assessments, "preview-")

        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.INITIAL
        ).execute()

        sendDocument(user.chatId.toChatId(), InputFile.fromFile(report.first), text = "Оцінки конкурсу ${activeCompetition.name.value} у форматі Microsoft Excel.", replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, true))
        report.first.delete()
    }
}