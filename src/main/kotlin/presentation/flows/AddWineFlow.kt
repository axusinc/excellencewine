package presentation.flows

import app.logger
import application.usecase.GetActiveCompetitionRequest
import application.usecase.GetUserByChatIdRequest
import application.usecase.UpdateConversationStateRequest
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.matrix
import domain.model.entity.User
import domain.model.entity.Vine
import domain.model.value.ConversationState
import domain.ports.repositories.CompetitionRepository
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import infrastructure.persistence.tables.UsersTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings
import presentation.MenuUtils
import presentation.RetryUtils.tryWithRetry
import presentation.flows.AssessFlow.requestCompetitionCategoryPick
import presentation.flows.AssessFlow.requestCompetitionVinePick
import presentation.flows.CreateCompetitionFlow.COMPETITION_VINES_REQUESTED_METADATA
import presentation.flows.CreateCompetitionFlow.addCompetitionVines

object AddWineFlow {
    suspend fun <BC : BehaviourContext> BC.setupAddWineFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.ADD_WINE))) { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.HEAD_OF_EXPERTS)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Керівникам експертів.")
                return@onText
            }

            reply(message, "Ви обрали додати вино", replyMarkup = MenuUtils.BACK_BUTTON_MENU)

            reply(message, "Введіть нове вино у форматі 'sampleCode: vineType'")

            UpdateConversationStateRequest(
                user.chatId!!,
                ConversationState.COMPETITION_ADD_NEW_VINE_REQUESTED
            ).execute()
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onText { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if (user?.conversationState != ConversationState.COMPETITION_ADD_NEW_VINE_REQUESTED)
                return@onText

            addCompetitionVine(user, message)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }
    }

    suspend fun <BC : BehaviourContext> BC.addCompetitionVine(
        user: User,
        message: AccessibleMessage,
    ) {
        val activeCompetition = GetActiveCompetitionRequest().execute() ?: run {
            reply(message, "Немає активного конкурсу. Будь ласка, створіть конкурс перед додаванням вин.")
            return
        }
        val newVine = message.text!!.let { vineString ->
            if (vineString.isBlank()) return
            val parts = vineString.split(":").map { it.trim() }
            if (parts.size != 2) {
                reply(message, "Неправильне вино (частин більше за 2)")
            }
            return@let try {
                val newVine = Vine(null, null, Vine.Type.RED, Vine.SampleCode(parts[0]), Vine.RealType.fromString(parts[1]) ?: run {
                    reply(message, "Неправильний тип вина (${parts[1]}) -- $vineString")
                    return
                })
                if(activeCompetition.vines.any { it.sampleCode == newVine.sampleCode }) {
                    reply(message, "Це вино вже додано -- $vineString")
                    return
                } else newVine
            } catch (e: Exception) {
                throw e
            }
        }

        val competitionRepository: CompetitionRepository by inject(CompetitionRepository::class.java)
        competitionRepository.update(activeCompetition.copy(vines = activeCompetition.vines + newVine))!!
        reply(message, "Успішно!")

        newSuspendedTransaction {
            UsersTable.selectAll().forEach { row ->
                try {
                    tryWithRetry(3, 1000) {
                        val rowUser = UsersTable.fromRow(row)
                        if(rowUser.chatId != null) {
                            UpdateConversationStateRequest(rowUser.chatId, ConversationState.INITIAL).execute()
                            send(rowUser.chatId.toChatId(), "Нове вино подано на оцінку -- ${newVine.sampleCode.value}.", replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, rowUser.role, true))
                            if(rowUser.role == User.Role.EXPERT) requestCompetitionCategoryPick(rowUser, newVine)
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to send competition started message to user $row" }
                }
            }
        }
    }
}