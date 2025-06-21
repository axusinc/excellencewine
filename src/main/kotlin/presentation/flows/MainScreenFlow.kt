package presentation.flows

import application.usecase.GetActiveCompetitionRequest
import application.usecase.UpdateConversationStateRequest
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import domain.model.entity.User
import domain.model.value.ConversationState
import presentation.MenuUtils

object MainScreenFlow {
    suspend fun <BC : BehaviourContext> BC.showMainScreen(user: User, message: AccessibleMessage) {
        UpdateConversationStateRequest(user.chatId!!, ConversationState.INITIAL).execute()
        val activeCompetition = GetActiveCompetitionRequest().execute()

        val mainMenu = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, activeCompetition != null)
        reply(message, "Почнімо все спочатку!", replyMarkup = mainMenu)
    }
}