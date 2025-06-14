package presentation

import application.usecase.UpdateConversationStateRequest
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import presentation.admin.generateAdminMenu

object ConversationStateUtils {
    suspend fun <BC : BehaviourContext> BC.answerIncorrectConversationState(triggerMessage: AccessibleMessage) {
        val repliedMessage = reply(triggerMessage, "Будь ласка, спочатку завершіть поточну розмову. Це повідомлення буде знищено за 5 секунд.")
        launch {
            delay(5000)
            deleteMessage(triggerMessage.chat.id, repliedMessage.messageId)
            deleteMessage(triggerMessage.chat.id, triggerMessage.messageId)
        }
    }
}