package presentation

import app.logger
import application.usecase.*
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContact
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.RequestContactKeyboardButton
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState

object StartBotFlow {
    @Suppress("t")
    suspend fun <BC : BehaviourContext> BC.setupStartBotFlow() {
        onCommand("start") { message -> try {
            val keyboard = ReplyKeyboardMarkup(
                listOf(
                    listOf(
                        RequestContactKeyboardButton("📞 Надати номер телефону")
                    )
                ),
                resizeKeyboard = true,
                oneTimeKeyboard = false
            )
            reply(
                message,
                "Надайте свій номер телефону натиснувши на кнопку \"📞 Надати номер телефону\" в меню знизу.",
                replyMarkup = keyboard
            )
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onContact { contact -> try {
            logger.info { "Received contact: $contact" }
            val activeCompetition = GetActiveCompetitionRequest().execute()

            val phoneNumber =
                User.PhoneNumber(contact.content.contact.phoneNumber.let { if (!it.startsWith("+")) "+$it" else it })
            val registeredAdmins = FilterUsersByRoleRequest(User.Role.ADMIN).execute()
            if (registeredAdmins.isEmpty()) {
                val user = CreateUserRequest(
                    User(
                        phoneNumber = phoneNumber,
                        chatId = contact.chat.id.chatId,
                        name = User.Name(contact.content.contact.firstName),
                        role = User.Role.ADMIN,
                        conversationState = ConversationState.INITIAL,
                        conversationMetadata = ConversationMetadata("")
                    )
                ).execute()
                reply(
                    contact,
                    "Вітаємо, ${user.name.value}! Успішно зареєстровано як адміністратора.",
                    replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, activeCompetition != null)
                )
            } else {
                val user = GetUserRequest(phoneNumber).execute()
                if (user != null && user.chatId == null) {
                    val user =
                        UpdateUserRequest(user.copy(chatId = contact.chat.id.chatId)).execute()!! // Must be not null if the operation is successful
                    reply(
                        contact,
                        "Вітаємо, ${user.name.value}! Ваш номер телефону успішно зареєстровано як ${user.role}.",
                        replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, activeCompetition != null)
                    )
                } else if (user == null) {
                    reply(contact, "Ви не зареєстровані. Спочатку Адміністратор має внести Вас в систему.")
                } else {
                    reply(
                        contact,
                        "Ви вже зареєстровані. Продовжуйте просто користуватися ботом. Якщо це помилка, зверніться до адміністратора.",
                        replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, activeCompetition != null)
                    )
                }
            }
        } catch (e: Exception) { e.printStackTrace(); reply(contact, CommonStrings.ERROR_UNKNOWN) } }
    }
}