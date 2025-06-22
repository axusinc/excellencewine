package presentation.flows

import UserAlreadyExistsException
import app.logger
import application.usecase.CreateUserRequest
import application.usecase.FilterUsersByRoleRequest
import application.usecase.GetUserByChatIdRequest
import application.usecase.UpdateConversationStateRequest
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.types.toChatId
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationMetadata.Companion.toConversationMetadata
import domain.model.value.ConversationState
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import presentation.CommonStrings
import presentation.FlowUtils.sendIncorrectStateMessage
import presentation.MenuUtils

// Pls dont look at file's LOC
object AddUserFlow {
    @Suppress("t")
    suspend fun <BC : BehaviourContext> BC.setupAddUserFlow() {
        onText { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            val withRole = CommonStrings.getRoleFromReadableName(message.content.text) ?: return@onText
            if (user.role != User.Role.ADMIN) {
                reply(message, "Ця команда доступна лише адміністраторам.")
                return@onText
            }

            reply(message, "Ви обрали список всіх ${CommonStrings.getRoleReadableName(withRole)}.", replyMarkup = MenuUtils.BACK_BUTTON_MENU)

            displayUsersList(user, message, withRole)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery("add_user") { query -> try {
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if(user.conversationState != ConversationState.USERS_LIST_DISPLAYED){
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val userListDisplayMetadata = user.conversationMetadata.value.decodeObject<USERS_LIST_DISPLAYED_METADATA>()
            requestUserPhoneNumber(user, userListDisplayMetadata.withRole)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onText { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if (user?.conversationState != ConversationState.USER_PHONE_NUMBER_REQUESTED)
                return@onText
            val requestUserPhoneNumberMetadata = user.conversationMetadata.value.decodeObject<USER_PHONE_NUMBER_REQUESTED_METADATA>()

            val phoneNumber = try {
                User.PhoneNumber(message.content.text)
            } catch (_: User.PhoneNumber.IsInvalidException) {
                reply(message, "Будь ласка, надішліть коректний номер телефону у форматі +XXXXXXXXXXXX.")
                return@onText
            }

            logger.info { "Waiting for User Name of new user with phone number: $phoneNumber" }
            requestUserName(user, message, requestUserPhoneNumberMetadata.withRole, phoneNumber)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onText { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if (user?.conversationState != ConversationState.USER_NAME_REQUESTED)
                return@onText
            val requestUserNameMetadata = user.conversationMetadata.value.decodeObject<USER_NAME_REQUESTED_METADATA>()

            val name = try {
                User.Name(message.content.text.trim())
            } catch (_: User.Name.IsInvalidException) {
                reply(message, "Будь ласка, надішліть коректне ім'я нового користувача (не більше ніж 28 символів).")
                return@onText
            }
            logger.info { "Adding new user with $requestUserNameMetadata and name \"$name\"" }

            return@onText try {
                CreateUserRequest(
                    User(
                        phoneNumber = requestUserNameMetadata.phoneNumber,
                        chatId = null,
                        name = name,
                        role = requestUserNameMetadata.withRole,
                        conversationState = ConversationState.INITIAL,
                        conversationMetadata = ConversationMetadata("")
                    )
                ).execute()
                reply(
                    message,
                    "Користувача ${name.value} з номером ${requestUserNameMetadata.phoneNumber.value} успішно зареєстровано як ${CommonStrings.getRoleReadableName(requestUserNameMetadata.withRole)}."
                )
                requestUserPhoneNumber(user, requestUserNameMetadata.withRole)

            } catch (_: UserAlreadyExistsException) {
                reply(message, "Користувач з номером ${requestUserNameMetadata.phoneNumber.value} вже зареєстрований.")
                requestUserPhoneNumber(user, requestUserNameMetadata.withRole)
            } catch (e: Exception) {
                logger.error { "Error while adding user: ${e.stackTraceToString()}" }
                reply(
                    message,
                    "Виникла невідома помилка при додаванні нового користувача. Будь ласка, спробуйте ще раз."
                )
                requestUserPhoneNumber(user, requestUserNameMetadata.withRole)
            }
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }
    }




    data class USERS_LIST_DISPLAYED_METADATA(
        val withRole: User.Role
    )
    suspend fun <BC : BehaviourContext> BC.displayUsersList(user: User, message: AccessibleMessage, withRole: User.Role) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.USERS_LIST_DISPLAYED,
            USERS_LIST_DISPLAYED_METADATA(withRole).toConversationMetadata()
        ).execute()

        val registeredUsers = FilterUsersByRoleRequest(withRole).execute()
        val inlineMarkup = InlineKeyboardMarkup(
            CallbackDataInlineKeyboardButton(
                "Додати ${CommonStrings.getRoleReadableName(withRole)}",
                "add_user"
            )
        )
        val sentMessage = reply(
            message,
            "Ось список зареєстрованих ${CommonStrings.getRoleReadableName(withRole)}:\n" +
                    registeredUsers.mapIndexed { index, registeredUser -> "${index + 1}. ${registeredUser.name.value} ${registeredUser.phoneNumber.value}" + if (registeredUser.chatId == null) " (ще не запускав бота)" else "" }
                        .joinToString("\n") +
                    (if (registeredUsers.isEmpty()) "\n(Список порожній)" else ""),
//                replyMarkup = MenuUtils.BACK_BUTTON_MENU
        )
//            delay(1000)
//            editMessageReplyMarkup(sentMessage, replyMarkup = replyMarkup)
        edit(user.chatId.toChatId(), sentMessage.messageId, replyMarkup = inlineMarkup)
    }

    data class USER_PHONE_NUMBER_REQUESTED_METADATA(
        val withRole: User.Role
    )
    suspend fun <BC : BehaviourContext> BC.requestUserPhoneNumber(user: User, withRole: User.Role) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.USER_PHONE_NUMBER_REQUESTED,
            USER_PHONE_NUMBER_REQUESTED_METADATA(withRole).toConversationMetadata()
        ).execute()

        sendMessage(
            user.chatId.toChatId(),
            "Будь ласка, надішліть номер телефону нового користувача у форматі +XXXXXXXXXXXX."
        )
    }

    data class USER_NAME_REQUESTED_METADATA(
        val withRole: User.Role,
        val phoneNumber: User.PhoneNumber
    )
    suspend fun <BC : BehaviourContext> BC.requestUserName(user: User, message: AccessibleMessage, withRole: User.Role, phoneNumber: User.PhoneNumber) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.USER_NAME_REQUESTED,
            USER_NAME_REQUESTED_METADATA(withRole, phoneNumber).toConversationMetadata()
        ).execute()

        reply(
            message,
            "Будь ласка, надішліть ім'я нового користувача."
        )
    }
}