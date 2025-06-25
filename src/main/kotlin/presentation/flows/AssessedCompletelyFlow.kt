package presentation.flows

import application.usecase.GetActiveCompetitionRequest
import application.usecase.GetUserByChatIdRequest
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import domain.model.entity.User
import domain.model.value.ConversationState
import domain.ports.repositories.VineAssessmentRepository
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings

object AssessedCompletelyFlow {
    suspend fun <BC : BehaviourContext> BC.setupAssessedCompletelyFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.ASSESSED_COMPLETELY))) { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.HEAD_OF_EXPERTS, User.Role.VINE_MAKER)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Керівникам експертів.")
                return@onText
            }

            val activeCompetition = GetActiveCompetitionRequest().execute()!!
            val lastVine = activeCompetition.vines.lastOrNull()
            val vineAssessmentRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
            val assessments = vineAssessmentRepository.filter(competitionId = activeCompetition.id)
            val assessedInCompletely = lastVine?.let { vine -> activeCompetition.experts.mapNotNull { expert -> if(assessments.filter { it.to == vine.id && it.from == expert.id }.size != vine.realType.getCategories().size) expert.name else null } }
            reply(message, "Останнє вино (${lastVine?.sampleCode?.value}) оцінено повністю: ${assessedInCompletely.isNullOrEmpty()}\n" +
                    "Експерти, які не оцінили це вино повністю: \n${assessedInCompletely?.joinToString("\n") { "- " + it.value } ?: "Всі експерти оцінили це вино повністю."}",)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }
    }
}