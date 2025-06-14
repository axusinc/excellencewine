package presentation

import CannotGetUserRoleException
import UserAlreadyExistsException
import app.logger
import application.usecase.GetUserRequest
import application.usecase.RegisterUserRequest
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContact
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.RequestContactKeyboardButton
import dev.inmo.tgbotapi.types.toChatId
import domain.model.entity.User
import domain.model.value.ConversationState
import domain.model.value.PhoneNumber

suspend fun <BC : BehaviourContext> BC.startBotFlow() {
    onCommand("start") { message ->
        val keyboard = ReplyKeyboardMarkup(
            listOf(
                listOf(
                    RequestContactKeyboardButton("📞 Надати номер телефону")
                )
            ),
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )
        reply(message, "Надайте свій номер телефону натиснувши на кнопку \"📞 Надати номер телефону\" в меню знизу.", replyMarkup = keyboard)
    }
    onContact { contact ->
        logger.info { "Received contact: $contact" }
        val phoneNumber = contact.content.contact.phoneNumber.let { if(!it.startsWith("+")) "+$it" else it }
        Result.runCatching {
            RegisterUserRequest(contact.chat.id.toChatId().toUserId(), PhoneNumber(phoneNumber)).execute()
        }
            .onSuccess { user -> send(user.id.toChatId(), "Вітаємо, ${user.name.value}! Успішно зареєстровано як ${user.role}.", replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role)) }
            .onFailure { error -> when(error) {
                is UserAlreadyExistsException -> {
                    val user = GetUserRequest(contact.chat.id.toChatId().toUserId()).execute()!!
                    reply(
                        contact,
                        "Ви вже зареєстровані. " +
                                "Якщо це помилка, зверніться до адміністратора.",
                        replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role)
                    )
                }
                is CannotGetUserRoleException -> reply(
                    contact,
                    "Ваш номер телефону не записаний в системі адміністратором. " +
                            "Будь ласка, зверніться до адміністратора.",
                    replyMarkup = MenuUtils.EMPTY_MENU
                )
                else -> {
                    logger.error { error.stackTraceToString() }
                    reply(
                        contact,
                        "Виникла невідома помилка при реєстрації. " +
                                "Будь ласка, спробуйте ще раз або зверніться до адміністратора.",
                        replyMarkup = MenuUtils.EMPTY_MENU
                    )
                }
            } }
    }
}