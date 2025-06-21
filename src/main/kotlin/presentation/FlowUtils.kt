package presentation

import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.delay

object FlowUtils {
    suspend fun <BC : BehaviourContext> BC.sendIncorrectStateMessage(userChatId: RawChatId) {
        val sentMessage = send(
            userChatId.toChatId(),
            "Будь ласка, завершіть спочатку поточну розмову. Це повідомлення самознищиться через 5 секунд."
        )
        delay(5 * 1000L)
        delete(sentMessage)
    }
}