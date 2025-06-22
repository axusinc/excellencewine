package presentation.flows

import application.usecase.*
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.toChatId
import domain.model.entity.Competition
import domain.model.entity.User
import domain.model.entity.Vine
import domain.model.value.ConversationState
import domain.ports.repositories.CompetitionRepository
import domain.ports.repositories.VineAssessmentRepository
import eth.likespro.commons.models.Pagination
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings
import presentation.FlowUtils.sendIncorrectStateMessage
import presentation.InlineMarkupPaginationUtils
import presentation.MenuUtils
import presentation.ReportUtils.computeMedal
import presentation.ReportUtils.generateReport

object GetCompetitionResultsFlow {
    suspend fun <BC : BehaviourContext> BC.setupGetCompetitionResultsFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.COMPETITIONS))) { message -> try {
            if (message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
//            if (user.role !in listOf(User.Role.ADMIN, User.Role.HEAD_OF_EXPERTS)) {
//                reply(message, "Ця команда доступна лише Адміністраторам та Керівникам експертів.")
//                return@onText
//            }

            reply(message, "Ви обрали список всіх конкурсів.", replyMarkup = MenuUtils.BACK_BUTTON_MENU)

            displayCompetitionsList(user, message)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_competition"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.COMPETITIONS_LIST_DISPLAYED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            send(user.chatId!!.toChatId(), "Таблиця з оцінками готується...")

            sendCompetitionResults(user, Competition.Id(query.data.split("_").first()))
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }
    }




    suspend fun <BC : BehaviourContext> BC.displayCompetitionsList(user: User, message: AccessibleMessage) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITIONS_LIST_DISPLAYED
        ).execute()

        val competitions = GetCompetitionsRequest().execute()

        val newUser = user.copy(
            currentInlineMarkupButtons = competitions.map { listOf(CallbackDataInlineKeyboardButton((if(it.vineType == Vine.Type.RED) "🍷" else "🍾") + " " + it.name.value, it.id.value + "_competition")) },
            inlineMarkupPagination = Pagination(0, 12)
        )

        UpdateInlineMarkupStateRequest(
            user.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        ).execute()

        reply(
            message,
            "Ось список недавніх змагань:",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }

    suspend fun <BC : BehaviourContext> BC.sendCompetitionResults(user: User, competitionId: Competition.Id) {
        val competitionRepository: CompetitionRepository by inject(CompetitionRepository::class.java)
        val competition = competitionRepository.findById(competitionId)!!
        val assessmentsRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
        val assessments = assessmentsRepository.filter(competitionId = competition.id)
        val report = generateReport(competition, assessments, "report-")

        val activeCompetition = GetActiveCompetitionRequest().execute()

        sendDocument(user.chatId!!.toChatId(), InputFile.fromFile(report.first), text = "Таблиця з результатами конкурсу ${competition.name.value} прикріплена нижче.\n" +
                "Здобуті місця:\n" +
                report.second.mapNotNull { computeMedal(it.value)?.let { medal ->
                    "$medal: ${it.key.value} (${it.value})"
                } }.sorted().joinToString("\n"), replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, activeCompetition != null))

        UpdateConversationStateRequest(
            user.chatId,
            ConversationState.INITIAL
        ).execute()

        report.first.delete()
    }
}