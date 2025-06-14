package presentation.admin

import NotRegisteredUserAlreadyExistsException
import UserAlreadyExistsException
import app.logger
import application.usecase.CreateNotActivatedUserRequest
import application.usecase.FilterNotActivatedUsersByRoleRequest
import application.usecase.FilterUsersByRoleRequest
import application.usecase.GetUserRequest
import application.usecase.UpdateConversationStateRequest
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.toChatId
import domain.model.entity.NotActivatedUser
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import domain.model.value.PhoneNumber
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import presentation.ConversationStateUtils.answerIncorrectConversationState
import presentation.toChatId
import presentation.toUserId

// Pls dont look at file's LOC
@Suppress("t")
suspend fun <BC : BehaviourContext> BC.addHeadsOfExpertsFlow() {
    onText(CommonMessageFilterIncludeText(Regex("Керівники експертів"))) { message ->
        val user = GetUserRequest(message.chat.id.toChatId().toUserId()).execute()
        if(user?.role != User.Role.ADMIN) {
            reply(message, "Ця команда доступна лише адміністраторам.")
            return@onText
        }
        UpdateConversationStateRequest(user.id, ConversationState.INITIAL).execute()
//        if(user.conversationState != ConversationState.INITIAL) {
//            answerIncorrectConversationState(message)
//            return@onText
//        }

        val registeredHeadsOfExperts = FilterUsersByRoleRequest(User.Role.HEAD_OF_EXPERTS).execute()
        val notActivedHeadsOfExperts = FilterNotActivatedUsersByRoleRequest(User.Role.HEAD_OF_EXPERTS).execute()
        val replyMarkup = InlineKeyboardMarkup(
            CallbackDataInlineKeyboardButton("Додати керівника експертів", "add_head_of_expert"),
        )
        reply(message, "Ось список зареєстрованих керівників експертів:\n" +
                registeredHeadsOfExperts.mapIndexed { index, user -> "${index + 1}. ${user.name.value}" }.joinToString("\n") +
                (if(registeredHeadsOfExperts.isEmpty()) "(Список порожній)" else "") +
                "\n\nСписок неактивованих керівників експертів (ті, що ще не запустили бота):\n" +
                notActivedHeadsOfExperts.mapIndexed { index, user -> "${index + 1}. ${user.name.value}" }.joinToString("\n") +
                if(notActivedHeadsOfExperts.isEmpty()) "\n(Список порожній)" else ""
                , replyMarkup = replyMarkup)
    }

    suspend fun waitForHeadOfExpertsPhoneNumber(userId: User.Id) {
        sendMessage(userId.toChatId(), "Будь ласка, надішліть номер телефону нового керівника експертів у форматі +XXXXXXXXXXXX.")
        UpdateConversationStateRequest(userId, ConversationState.WAIT_FOR_HEAD_OF_EXPERTS_PHONE_NUMBER).execute()
    }

    onDataCallbackQuery("add_head_of_expert") { query -> waitForHeadOfExpertsPhoneNumber(query.user.id.toUserId()) }

    onText { message ->
        val user = GetUserRequest(message.chat.id.toChatId().toUserId()).execute()
        if(user?.conversationState != ConversationState.WAIT_FOR_HEAD_OF_EXPERTS_PHONE_NUMBER) {
//            answerIncorrectConversationState(message)
            return@onText
        }

        val phoneNumber = try {
            PhoneNumber(message.content.text)
        } catch (_: PhoneNumber.IsInvalidException) {
            reply(message, "Будь ласка, надішліть коректний номер телефону у форматі +XXXXXXXXXXXX.")
            return@onText
        }

        // Here you would typically add the new head of experts to the database
        // For demonstration purposes, we will just log it and send a confirmation message

        reply(message, "Будь ласка, надішліть ім'я керівника експертів.")
        UpdateConversationStateRequest(user.id, ConversationState.WAIT_FOR_HEAD_OF_EXPERTS_NAME, ConversationMetadata(phoneNumber.encodeObject())).execute()
    }

    onText { message -> try {
        val user = GetUserRequest(message.chat.id.toChatId().toUserId()).execute()
        if(user?.conversationState != ConversationState.WAIT_FOR_HEAD_OF_EXPERTS_NAME) {
//            answerIncorrectConversationState(message)
            return@onText
        }

        val name = User.Name(message.content.text)
        val phoneNumber = user.conversationMetadata.value.decodeObject<PhoneNumber>()
        logger.info { "Adding new head of experts with phone number: $phoneNumber and $name" }

        return@onText try {
            CreateNotActivatedUserRequest(
                NotActivatedUser(
                    name = name,
                    phoneNumber = phoneNumber,
                    role = User.Role.HEAD_OF_EXPERTS
                )
            ).execute()
            reply(message, "Керівника експертів ${name.value} з номером ${phoneNumber.value} успішно додано. " +
                    "Тепер він має запустити бота, щоб стати активним.")
            waitForHeadOfExpertsPhoneNumber(user.id)

        } catch (_: NotRegisteredUserAlreadyExistsException) {
            reply(message, "Керівник експертів з номером ${phoneNumber.value} вже доданий в систему. " +
                    "Тепер він має запустити бота, щоб стати активним.")
            waitForHeadOfExpertsPhoneNumber(user.id)
        } catch (_: UserAlreadyExistsException) {
            reply(message, "Керівник експертів з номером ${phoneNumber.value} вже зареєстрований.")
            waitForHeadOfExpertsPhoneNumber(user.id)
        } catch (e: Exception) {
            logger.error { "Error while adding head of experts: ${e.stackTraceToString()}" }
            reply(message, "Виникла невідома помилка при додаванні керівника експертів. Будь ласка, спробуйте ще раз.")
            waitForHeadOfExpertsPhoneNumber(user.id)
        }
    } catch (e: Exception) {e.printStackTrace()} }
}