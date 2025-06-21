package presentation.flows

import application.usecase.GetCompetitionsRequest
import application.usecase.GetUserByChatIdRequest
import application.usecase.UpdateConversationStateRequest
import application.usecase.UpdateInlineMarkupStateRequest
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import domain.model.entity.User
import domain.model.value.ConversationState
import eth.likespro.commons.models.Pagination
import presentation.CommonStrings
import presentation.InlineMarkupPaginationUtils
import presentation.MenuUtils

object GetCompetitionResultsFlow {
    suspend fun <BC : BehaviourContext> BC.setupGetCompetitionResultsFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.COMPETITIONS))) { message -> try {
            if (message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.HEAD_OF_EXPERTS)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Керівникам експертів.")
                return@onText
            }

            reply(message, "Ви обрали список всіх конкурсів.", replyMarkup = MenuUtils.BACK_BUTTON_MENU)

            displayCompetitionsList(user, message)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }
    }




    suspend fun <BC : BehaviourContext> BC.displayCompetitionsList(user: User, message: AccessibleMessage) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITIONS_LIST_DISPLAYED
        ).execute()

        val competitions = GetCompetitionsRequest().execute()

        val newUser = user.copy(
            currentInlineMarkupButtons = competitions.map { listOf(CallbackDataInlineKeyboardButton(it.name.value, it.id.value + "_competition")) },
            inlineMarkupPagination = Pagination(0, 10)
        )

        UpdateInlineMarkupStateRequest(
            user.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        )

        reply(
            message,
            "Ось список недавніх змагань:",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }
}