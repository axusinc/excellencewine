package presentation.flows

import application.usecase.*
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import domain.model.entity.Category
import domain.model.entity.User
import domain.model.entity.Vine
import domain.model.entity.VineAssessment
import domain.model.value.ConversationMetadata.Companion.toConversationMetadata
import domain.model.value.ConversationState
import domain.ports.repositories.VineAssessmentRepository
import eth.likespro.commons.models.Pagination
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings
import presentation.FlowUtils.sendIncorrectStateMessage
import presentation.InlineMarkupPaginationUtils
import presentation.MenuUtils

object AssessFlow {
    suspend fun <BC : BehaviourContext> BC.setupAssessFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.ASSESS))) { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.EXPERT)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Експертам.")
                return@onText
            }

            reply(message, "Ви обрали поставити оцінку", replyMarkup = MenuUtils.BACK_BUTTON_MENU)

            requestCompetitionVinePick(user, message)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_vine_pick") && !query.data.endsWith("_vine_pick_selected"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.COMPETITION_VINE_PICK_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val vineId = Vine.Id(query.data.split("_").first())

            requestCompetitionCategoryPick(user, vineId)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_category_pick") && !query.data.endsWith("_category_pick_selected"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.COMPETITION_CATEGORY_PICK_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val metadata = user.conversationMetadata.value.decodeObject<COMPETITION_CATEGORY_PICK_REQUESTED_METADATA>()
            val category = Category(Category.Name(query.data.split("_").first()))

            requestCompetitionVineMarkPick(user, metadata.vine, category)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_mark") && !query.data.endsWith("_mark_selected"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.COMPETITION_VINE_MARK_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val metadata = user.conversationMetadata.value.decodeObject<COMPETITION_VINE_MARK_REQUESTED_METADATA>()
            val mark = query.data.split("_").first().toInt()

            assessVine(user, metadata.vineId, metadata.category, mark)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }
    }







    suspend fun <BC : BehaviourContext> BC.requestCompetitionVinePick(user: User, message: AccessibleMessage) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_VINE_PICK_REQUESTED,
        ).execute()

        val activeCompetition = GetActiveCompetitionRequest().execute()!!
        val vines = activeCompetition.vines

        val newUser = user.copy(
            currentInlineMarkupButtons = vines.map { listOf(
                CallbackDataInlineKeyboardButton(
                it.name.value,
                it.id.toString() + "_vine_pick"
            )
            ) },
            inlineMarkupPagination = Pagination(0, 2)
        )

        UpdateInlineMarkupStateRequest(
            user.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        ).execute()

        reply(
            message,
            "Виберіть вино для оцінки нижче.",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }

    data class COMPETITION_CATEGORY_PICK_REQUESTED_METADATA(
        val vine: Vine.Id,
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionCategoryPick(user: User, vineId: Vine.Id) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_CATEGORY_PICK_REQUESTED,
            COMPETITION_CATEGORY_PICK_REQUESTED_METADATA(vineId).toConversationMetadata()
        ).execute()

        val activeCompetition = GetActiveCompetitionRequest().execute()!!
        val categories = activeCompetition.categories

        val newUser = user.copy(
            currentInlineMarkupButtons = categories.map { listOf(
                CallbackDataInlineKeyboardButton(
                    it.name.value,
                    it.id.value + "_category_pick"
                )
            ) },
            inlineMarkupPagination = Pagination(0, 2)
        )

        UpdateInlineMarkupStateRequest(
            user.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        ).execute()

        send(
            user.chatId.toChatId(),
            "Виберіть категорію для оцінки вина ${vineId.name.value} нижче.",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }

    data class COMPETITION_VINE_MARK_REQUESTED_METADATA(
        val vineId: Vine.Id,
        val category: Category,
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionVineMarkPick(user: User, vineId: Vine.Id, category: Category) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_VINE_MARK_REQUESTED,
            COMPETITION_VINE_MARK_REQUESTED_METADATA(vineId, category).toConversationMetadata()
        ).execute()

        val activeCompetition = GetActiveCompetitionRequest().execute()!!
        val marks = (1..5)

        val inlineMarkup = InlineKeyboardMarkup(matrix {
            row { marks.forEach { mark ->
                add(CallbackDataInlineKeyboardButton(mark.toString(), mark.toString() + "_mark"))
            } }
        })

        send(
            user.chatId.toChatId(),
            "Виберіть оцінку вина ${vineId.name.value} в категорії ${category.name.value} нижче.",
            replyMarkup = inlineMarkup
        )
    }

    suspend fun <BC : BehaviourContext> BC.assessVine(user: User, vineId: Vine.Id, category: Category, mark: Int) {
        val activeCompetition = GetActiveCompetitionRequest().execute()!!

        val vineAssessmentRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
        vineAssessmentRepository.upsert(VineAssessment(
            competitionId = activeCompetition.id,
            from = user.id,
            to = vineId,
            category = category.name,
            mark = mark
        ))

        send(user.chatId!!.toChatId(), "Ви успішно оцінили вино ${vineId.name.value} в категорії ${category.name.value} на $mark балів.")

        requestCompetitionCategoryPick(user, vineId)
    }
}