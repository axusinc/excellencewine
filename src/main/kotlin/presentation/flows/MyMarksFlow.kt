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
import io.github.evanrupert.excelkt.workbook
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings
import presentation.MenuUtils
import presentation.ReportUtils.computeRealMark
import presentation.XLSXUtils.aquaBackgroundStyle
import presentation.XLSXUtils.redBackgroundStyle
import java.io.File

object MyMarksFlow {
    suspend fun <BC : BehaviourContext> BC.setupMyMarksFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.MY_MARKS))) { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.EXPERT)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Експертам.")
                return@onText
            }

            reply(message, "Таблиця з вашими оцінками готується...")

            sendMyMarks(user, message)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }
    }





    suspend fun <BC : BehaviourContext> BC.sendMyMarks(user: User, message: AccessibleMessage) {
        val vineAssessmentRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
        val activeCompetition = runBlocking { GetActiveCompetitionRequest().execute()!! }
        val assessments = runBlocking { vineAssessmentRepository.filter(competitionId = activeCompetition.id, from = user.id) }
        val vines = activeCompetition.vines.sortedBy { it.name.value }

        val tmpFile = File("${activeCompetition.name.value}${user.id.value}-report.xlsx")
        workbook {
            sheet("Мої оцінки") {

                row{
                    cell("Вино")
                    activeCompetition.categories.forEach { category -> cell(category.name.value) }
                }

                vines.forEach { vine ->
                    row {
                        cell(vine.name.value)
                        activeCompetition.categories.forEach { category ->
                            val mark = assessments.find { it.to == vine.id && it.category == category.name }?.let { computeRealMark(it.category, it.mark) }
                            if(mark != null)
                                cell(mark.toString(), aquaBackgroundStyle())
                            else
                                cell("Немає оцінки", redBackgroundStyle())
                        }
                    }
                }
            }
        }.write(tmpFile.path)

        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.INITIAL
        ).execute()

        sendDocument(user.chatId.toChatId(), InputFile.fromFile(tmpFile), text = "Ваші оцінки у форматі Microsoft Excel.", replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, true))
        tmpFile.delete()
    }
}