package presentation

import application.usecase.GetUserByChatIdRequest
import application.usecase.UpdateInlineMarkupStateRequest
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.accessibleMessageOrThrow
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.Matrix
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import domain.model.entity.User
import eth.likespro.commons.models.Pagination.Companion.applyPagination

object InlineMarkupPaginationUtils {
    suspend fun <BC : BehaviourContext> BC.setupInlineMarkupPagination() {
        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_pagination_back"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            UpdateInlineMarkupStateRequest(user.chatId!!, user.currentInlineMarkupButtons, user.inlineMarkupPagination.previous()).execute()
            edit(query.message!!.accessibleMessageOrThrow(), replyMarkup = generateInlineMarkup(user.copy(inlineMarkupPagination = user.inlineMarkupPagination.previous())))
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_pagination_next"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            UpdateInlineMarkupStateRequest(user.chatId!!, user.currentInlineMarkupButtons, user.inlineMarkupPagination.next()).execute()
            edit(query.message!!.accessibleMessageOrThrow(), replyMarkup = generateInlineMarkup(user.copy(inlineMarkupPagination = user.inlineMarkupPagination.next())))
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }
    }



    fun generateInlineMarkup(user: User): InlineKeyboardMarkup = InlineKeyboardMarkup(matrix {
        user.currentInlineMarkupButtons.filter { buttonsRow ->
            buttonsRow.any { button -> !button.callbackData.contains("(always_on)") }
        }.applyPagination(user.inlineMarkupPagination).forEach { buttonsRow ->
            row {
                buttonsRow.map { button -> if(!button.callbackData.contains("(always_on)")) +button }
            }
        }
        row {
            if(user.inlineMarkupPagination.hasPrevious()) +CallbackDataInlineKeyboardButton(
                text = CommonStrings.BACK_BUTTON_TEXT,
                callbackData = "_pagination_back"
            )
            if(user.currentInlineMarkupButtons.count { row -> row.any { !it.callbackData.contains("(always_on)") } } > user.inlineMarkupPagination.next().offset) +CallbackDataInlineKeyboardButton(
                text = CommonStrings.NEXT_BUTTON_TEXT,
                callbackData = "_pagination_next"
            )
        }
        user.currentInlineMarkupButtons.forEach { buttonsRow ->
            if (buttonsRow.any { it.callbackData.contains("(always_on)") }) row {
                buttonsRow.filter { it.callbackData.contains("(always_on)") }.forEach { button ->
                    +button
                }
            }
        }
    })
}