package presentation.flows

import application.usecase.*
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.formatting.createMarkdownText
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
import presentation.ReportUtils.computeRealMark
import presentation.flows.MainScreenFlow.showMainScreen

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

            val vineId = Vine.SampleCode(query.data.split("_").first())

            val activeCompetition = GetActiveCompetitionRequest().execute()!!
            val vine = activeCompetition.vines.find { it.id == vineId }!!
            requestCompetitionCategoryPick(user, vine)
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

            send(user.chatId!!.toChatId(), "Ви обрали категорію **${category.name.value}** для оцінки.", replyMarkup = MenuUtils.BACK_BUTTON_MENU)
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

            assessVine(user, metadata.vine.id, metadata.category, mark)
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
                it.sampleCode.value,
                it.id.value + "_vine_pick"
            )
            ) },
            inlineMarkupPagination = Pagination(0, 12)
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
        val vine: Vine.SampleCode,
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionCategoryPick(user: User, vine: Vine) {
        val activeCompetition = GetActiveCompetitionRequest().execute()!!
        val assessmentRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
        val assessments = assessmentRepository.filter(
            competitionId = activeCompetition.id,
            to = vine.id,
            from = user.id
        )
        val categories = vine.realType.getCategories()

        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_CATEGORY_PICK_REQUESTED,
            COMPETITION_CATEGORY_PICK_REQUESTED_METADATA(vine.id).toConversationMetadata()
        ).execute()


        val newUser = user.copy(
            currentInlineMarkupButtons = categories.map { listOf(
                CallbackDataInlineKeyboardButton(
                    it.name.value.removeSuffix(" (Still wines)").removeSuffix(" (Sparkling wines)").removeSuffix(" (Spiritous wines)") + (assessments.find { assessment -> assessment.category == it.name }?.let { assessment -> " ✅ (${computeRealMark(it.name, assessment.mark)} балів)" } ?: " ❌"),
                    it.id.value + "_category_pick"
                )
            ) },
            inlineMarkupPagination = Pagination(0, 12)
        )

        UpdateInlineMarkupStateRequest(
            user.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        ).execute()

        send(
            user.chatId.toChatId(),
            "Виберіть категорію для оцінки вина **${vine.sampleCode.value}** нижче. На разі сума балів становить **${assessments.sumOf { computeRealMark(it.category, it.mark) }}**.",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }

    data class COMPETITION_VINE_MARK_REQUESTED_METADATA(
        val vine: Vine,
        val category: Category,
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionVineMarkPick(user: User, vineId: Vine.SampleCode, category: Category) {
        val activeCompetition = GetActiveCompetitionRequest().execute()!!
        val vine = activeCompetition.vines.find { it.id == vineId }!!

        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_VINE_MARK_REQUESTED,
            COMPETITION_VINE_MARK_REQUESTED_METADATA(vine, category).toConversationMetadata()
        ).execute()
        val marks = (1..5)

        val inlineMarkup = InlineKeyboardMarkup(matrix {
            row { marks.forEach { mark ->
                add(CallbackDataInlineKeyboardButton(computeRealMark(category.name, mark).toString(), mark.toString() + "_mark"))
            } }
        })

        send(
            user.chatId.toChatId(),
            text = "Виберіть оцінку вина **${vine.sampleCode.value}** в категорії **${category.name.value}** нижче.",
            replyMarkup = inlineMarkup,
        )
    }

    suspend fun <BC : BehaviourContext> BC.assessVine(user: User, vineId: Vine.SampleCode, category: Category, mark: Int) {
        val activeCompetition = GetActiveCompetitionRequest().execute()!!
        val vine = activeCompetition.vines.find { it.id == vineId }!!

        val vineAssessmentRepository: VineAssessmentRepository by inject(VineAssessmentRepository::class.java)
        vineAssessmentRepository.upsert(VineAssessment(
            competitionId = activeCompetition.id,
            from = user.id,
            to = vine.id,
            category = category.name,
            mark = mark
        ))

        send(user.chatId!!.toChatId(), "Ви успішно оцінили вино ${vine.sampleCode.value} в категорії **${category.name.value}** на ${computeRealMark(category.name, mark)} балів.")

        val categories = vine.realType.getCategories()

        val index = categories.indexOf(category)
        if(index < categories.size - 1) requestCompetitionVineMarkPick(user, vine.id, categories[index + 1])
        else {
            UpdateConversationStateRequest(
                user.chatId,
                ConversationState.INITIAL
            ).execute()

//            send(
//                user.chatId.toChatId(),
//                "Ви успішно оцінили вино ${vine.sampleCode.value} в деяких категоріях.",
//                replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, user.role, true)
//            )

            requestCompetitionCategoryPick(user, vine)
        }
    }
}