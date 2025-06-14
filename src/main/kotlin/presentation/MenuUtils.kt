package presentation

import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import domain.model.entity.User
import domain.model.value.ConversationState
import presentation.admin.generateAdminMenu

object MenuUtils {
    val EMPTY_MENU get() = ReplyKeyboardMarkup(listOf(), resizeKeyboard = true, oneTimeKeyboard = false)
    fun generateMenu(conversationState: ConversationState, role: User.Role): ReplyKeyboardMarkup = when (conversationState) {
        ConversationState.INITIAL -> {
            if (role == User.Role.ADMIN) generateAdminMenu()
            else generateAdminMenu()
        }
        else -> generateAdminMenu()
    }
}