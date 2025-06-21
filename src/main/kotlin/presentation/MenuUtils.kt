package presentation

import dev.inmo.tgbotapi.types.buttons.*
import domain.model.entity.User
import domain.model.value.ConversationState
import presentation.admin.generateAdminCompetitionActiveMenu
import presentation.admin.generateAdminMenu
import presentation.headofexperts.generateHeadOfExpertsActiveMenu
import presentation.headofexperts.generateHeadOfExpertsMenu

object MenuUtils {
    val EMPTY_INLINE_MENU get() = InlineKeyboardMarkup(emptyList())
    val EMPTY_MENU get() = ReplyKeyboardRemove()
    val BACK_BUTTON_MENU get() = ReplyKeyboardMarkup(listOf(listOf(SimpleKeyboardButton(CommonStrings.BACK_BUTTON_TEXT))), oneTimeKeyboard = false, resizeKeyboard = true)

    fun generateMenu(conversationState: ConversationState, role: User.Role, isCompetitionActive: Boolean): KeyboardMarkup = when (conversationState) {
        ConversationState.INITIAL -> { when(role) {
            User.Role.ADMIN -> if(!isCompetitionActive) generateAdminMenu() else generateAdminCompetitionActiveMenu()
            User.Role.HEAD_OF_EXPERTS -> if(!isCompetitionActive) generateHeadOfExpertsMenu() else generateHeadOfExpertsActiveMenu()
            else -> EMPTY_MENU
        } }
        else -> EMPTY_MENU
    }
}