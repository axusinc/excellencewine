package presentation.flows

import app.logger
import application.usecase.*
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterIncludeText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.accessibleMessageOrThrow
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import domain.model.entity.Category
import domain.model.entity.Competition
import domain.model.entity.User
import domain.model.entity.Vine
import domain.model.value.ConversationMetadata.Companion.toConversationMetadata
import domain.model.value.ConversationState
import domain.ports.repositories.CompetitionRepository
import eth.likespro.commons.models.Pagination
import eth.likespro.commons.models.value.Timestamp
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import infrastructure.persistence.tables.UsersTable
import kotlinx.coroutines.flow.asFlow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject
import presentation.CommonStrings
import presentation.FlowUtils.sendIncorrectStateMessage
import presentation.InlineMarkupPaginationUtils
import presentation.MenuUtils
import presentation.RetryUtils.tryWithRetry

object CreateCompetitionFlow {
    suspend fun <BC : BehaviourContext> BC.setupCreateCompetitionFlow() {
        onText(CommonMessageFilterIncludeText(Regex(CommonStrings.START_COMPETITION))) { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if(user?.conversationState != ConversationState.INITIAL)
                return@onText
            if (user.role !in listOf(User.Role.ADMIN, User.Role.HEAD_OF_EXPERTS)) {
                reply(message, "Ця команда доступна лише Адміністраторам та Керівникам експертів.")
                return@onText
            }

            reply(message, "Ви обрали почати новий конкурс", replyMarkup = MenuUtils.BACK_BUTTON_MENU)

            requestCompetitionName(user, message)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onText { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if (user?.conversationState != ConversationState.COMPETITION_NAME_REQUESTED)
                return@onText

            val competitionName = try {
                Competition.Name(message.content.text)
            } catch (_: Competition.Name.IsInvalidException) {
                reply(message, "Будь ласка, надішліть коректну назву конкурсу. Вона повинна бути не довше 40 символів")
                return@onText
            }

            logger.info { "Waiting for Competition Vine Type of new competition with $competitionName" }

            requestCompetitionVineType(user, message, competitionName)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery("red_vine_type") { query -> try {
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if(user.conversationState != ConversationState.COMPETITION_VINE_TYPE_REQUESTED){
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val userCompetitionVineTypeRequestedMetadata = user.conversationMetadata.value.decodeObject<COMPETITION_VINE_TYPE_REQUESTED_METADATA>()
            requestCompetitionExperts(user, userCompetitionVineTypeRequestedMetadata.name, Vine.Type.RED)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery("white_vine_type") { query -> try {
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if(user.conversationState != ConversationState.COMPETITION_VINE_TYPE_REQUESTED){
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val userCompetitionVineTypeRequestedMetadata = user.conversationMetadata.value.decodeObject<COMPETITION_VINE_TYPE_REQUESTED_METADATA>()
            requestCompetitionExperts(user, userCompetitionVineTypeRequestedMetadata.name, Vine.Type.WHITE)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        // Select an expert from the list
        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_expert") && !query.data.endsWith("_expert_selected"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.COMPETITION_EXPERTS_SELECTION_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val deselect = query.data.endsWith("_selected")
            val userCompetitionExpertsSelectionRequestedMetadata =
                user.conversationMetadata.value.decodeObject<COMPETITION_EXPERTS_SELECTION_REQUESTED_METADATA>()
            val expertPhoneNumber = User.PhoneNumber(query.data.split("_").first())

            val newConversationMetadata = userCompetitionExpertsSelectionRequestedMetadata.copy(
                selectedExperts = userCompetitionExpertsSelectionRequestedMetadata.selectedExperts.let {
                    if(!deselect) it + expertPhoneNumber
                    else it - expertPhoneNumber
                }
            )
            val newUser = user.copy(
                conversationMetadata = newConversationMetadata.toConversationMetadata(),
                currentInlineMarkupButtons = user.currentInlineMarkupButtons.map { row ->
                    row.map {
                        if (it.callbackData.startsWith(expertPhoneNumber.value + "_expert" /* after this _selected also can be */)) CallbackDataInlineKeyboardButton(
                            if(!deselect) it.text + " ✅" else it.text.removeSuffix(" ✅"),
                            if(!deselect) it.callbackData + "_selected" else it.callbackData.removeSuffix("_selected")
                        ) else it
                    }
                }.let {
                    if(newConversationMetadata.selectedExperts.size == 1 && !deselect) it + listOf(
                        listOf(CallbackDataInlineKeyboardButton("✅ Завершити вибір експертів", "(always_on)finish_experts_selection"))
                    ) else if(newConversationMetadata.selectedExperts.isEmpty() && deselect) it - listOf(
                        listOf(CallbackDataInlineKeyboardButton("✅ Завершити вибір експертів", "(always_on)finish_experts_selection"))
                    ).toSet()
                    else it
                }
            )
            UpdateInlineMarkupStateRequest(
                newUser.chatId!!,
                newUser.currentInlineMarkupButtons,
                newUser.inlineMarkupPagination
            ).execute()
            UpdateConversationStateRequest(newUser.chatId, ConversationState.COMPETITION_EXPERTS_SELECTION_REQUESTED, newUser.conversationMetadata).execute()

            edit(query.message!!.accessibleMessageOrThrow(), replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser))
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        // WOW, what a code duplication, but I don't want to create a new function for this

        onDataCallbackQuery { query -> try {
            if(query.data != "(always_on)finish_experts_selection")
                return@onDataCallbackQuery

            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if(user.conversationState != ConversationState.COMPETITION_EXPERTS_SELECTION_REQUESTED){
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val competitionExpertsSelectionRequestMetadata = user.conversationMetadata.value.decodeObject<COMPETITION_EXPERTS_SELECTION_REQUESTED_METADATA>()
//            requestCompetitionCategories(
//                user,
//                competitionExpertsSelectionRequestMetadata.name,
//                competitionExpertsSelectionRequestMetadata.vineType,
//                competitionExpertsSelectionRequestMetadata.selectedExperts
//            )
            requestCompetitionVines(
                user,
                competitionExpertsSelectionRequestMetadata.name,
                competitionExpertsSelectionRequestMetadata.vineType,
                competitionExpertsSelectionRequestMetadata.selectedExperts,
                emptyList()
            )
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        // Select a category from the list
        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_category") && !query.data.endsWith("_category_selected"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.COMPETITION_CATEGORIES_SELECTION_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val deselect = query.data.endsWith("_selected")
            val competitionCategoriesSelectionRequestedMetadata =
                user.conversationMetadata.value.decodeObject<COMPETITION_CATEGORIES_SELECTION_REQUESTED_METADATA>()
            val categoryName = Category.Name(query.data.split("_").first())

            val newConversationMetadata = competitionCategoriesSelectionRequestedMetadata.copy(
                selectedCategories = competitionCategoriesSelectionRequestedMetadata.selectedCategories!!.let {
                    if(!deselect) it + categoryName
                    else it - categoryName
                }
            )
            val newUser = user.copy(
                conversationMetadata = newConversationMetadata.toConversationMetadata(),
                currentInlineMarkupButtons = user.currentInlineMarkupButtons.map { row ->
                    row.map {
                        if (it.callbackData.startsWith(categoryName.value + "_category" /* after this _selected also can be */)) CallbackDataInlineKeyboardButton(
                            if(!deselect) it.text + " ✅" else it.text.removeSuffix(" ✅"),
                            if(!deselect) it.callbackData + "_selected" else it.callbackData.removeSuffix("_selected")
                        ) else it
                    }
                }.let {
                    if(newConversationMetadata.selectedCategories.size == 1 && !deselect) it + listOf(
                        listOf(CallbackDataInlineKeyboardButton("✅ Завершити вибір категорій", "(always_on)finish_categories_selection"))
                    ) else if(newConversationMetadata.selectedCategories.isEmpty() && deselect) it - listOf(
                        listOf(CallbackDataInlineKeyboardButton("✅ Завершити вибір категорій", "(always_on)finish_categories_selection"))
                    ).toSet()
                    else it
                }
            )
            UpdateInlineMarkupStateRequest(
                newUser.chatId!!,
                newUser.currentInlineMarkupButtons,
                newUser.inlineMarkupPagination
            ).execute()
            UpdateConversationStateRequest(newUser.chatId, ConversationState.COMPETITION_CATEGORIES_SELECTION_REQUESTED, newUser.conversationMetadata).execute()

            edit(query.message!!.accessibleMessageOrThrow(), replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser))
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery { query -> try {
            if(query.data != "(always_on)finish_categories_selection")
                return@onDataCallbackQuery

            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if(user.conversationState != ConversationState.COMPETITION_CATEGORIES_SELECTION_REQUESTED){
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val competitionCategoriesSelectionRequestMetadata = user.conversationMetadata.value.decodeObject<COMPETITION_CATEGORIES_SELECTION_REQUESTED_METADATA>()
            requestCompetitionVines(
                user,
                competitionCategoriesSelectionRequestMetadata.name,
                competitionCategoriesSelectionRequestMetadata.vineType,
                competitionCategoriesSelectionRequestMetadata.selectedExperts,
                competitionCategoriesSelectionRequestMetadata.selectedCategories
            )
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onText { message -> try {
            if(message.content.text == CommonStrings.BACK_BUTTON_TEXT)
                return@onText
            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()
            if (user?.conversationState != ConversationState.COMPETITION_VINES_REQUESTED)
                return@onText

            val competitionVinesRequestedMetadata = user.conversationMetadata.value.decodeObject<COMPETITION_VINES_REQUESTED_METADATA>()

            addCompetitionVines(user, message, competitionVinesRequestedMetadata)
        } catch (e: Exception) { e.printStackTrace(); reply(message, CommonStrings.ERROR_UNKNOWN) } }

        // Select a category from the list
        onDataCallbackQuery { query -> try {
            if(!query.data.endsWith("_vine") && !query.data.endsWith("_vine_selected"))
                return@onDataCallbackQuery
            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if (user.conversationState != ConversationState.COMPETITION_VINES_REQUESTED) {
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val competitionVinesSelectionRequestedMetadata =
                user.conversationMetadata.value.decodeObject<COMPETITION_VINES_REQUESTED_METADATA>()
            val vineId = Vine.SampleCode(query.data.split("_").first())

            val newConversationMetadata = competitionVinesSelectionRequestedMetadata.copy(
                selectedVines = competitionVinesSelectionRequestedMetadata.selectedVines -
                        competitionVinesSelectionRequestedMetadata.selectedVines.find { it.id == vineId }!!
            )
            val newUser = user.copy(
                conversationMetadata = newConversationMetadata.toConversationMetadata(),
                currentInlineMarkupButtons = user.currentInlineMarkupButtons.map { row ->
                    row.mapNotNull {
                        if (it.callbackData.startsWith(vineId.value + "_vine" /* after this _selected also can be */)) null // remove the button on click
                        else it
                    }
                }.let {
                    if(newConversationMetadata.selectedVines.isEmpty()) it - listOf(
                        listOf(CallbackDataInlineKeyboardButton("✅ Завершити вибір вин", "(always_on)finish_vines_selection"))
                    ).toSet() else it
                }
            )
            UpdateInlineMarkupStateRequest(
                newUser.chatId!!,
                newUser.currentInlineMarkupButtons,
                newUser.inlineMarkupPagination
            ).execute()
            UpdateConversationStateRequest(newUser.chatId, ConversationState.COMPETITION_VINES_REQUESTED, newUser.conversationMetadata).execute()

            edit(query.message!!.accessibleMessageOrThrow(), replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser))
            if(newConversationMetadata.selectedVines.isEmpty())  edit(query.user.id, query.message!!.messageId, "Всі попередньо додані вина були видалені. Введіть вина, які будуть оцінюватися на конкурсі у форматі \"<Sample Code> : <Vine Type>\" (без кавичок). Vine Type може бути \"STILL\", \"SPARKLING\" та \"SPIRITOUS\". Можна вводити декілька вин за один раз -- кожне в новому рядку.\n" +
                    "Наприклад:\n" +
                    "Sample Code 1 : STILL\n" +
                    "Sample Code 2 : SPARKLING\n" +
                    "Sample Code 3 : SPIRITOUS\n",)
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }

        onDataCallbackQuery { query -> try {
            if(query.data != "(always_on)finish_vines_selection")
                return@onDataCallbackQuery

            val user = GetUserByChatIdRequest(query.user.id.chatId).execute()!!
            if(user.conversationState != ConversationState.COMPETITION_VINES_REQUESTED){
                sendIncorrectStateMessage(user.chatId!!)
                return@onDataCallbackQuery
            }

            val competitionVinesSelectionRequestMetadata = user.conversationMetadata.value.decodeObject<COMPETITION_VINES_REQUESTED_METADATA>()
            createCompetition(
                user,
                competitionVinesSelectionRequestMetadata.name,
                competitionVinesSelectionRequestMetadata.vineType,
                competitionVinesSelectionRequestMetadata.selectedExperts.map { GetUserRequest(it).execute()!! },
                competitionVinesSelectionRequestMetadata.selectedCategories.map { Category(it) },
                competitionVinesSelectionRequestMetadata.selectedVines
            )
        } catch (e: Exception) { e.printStackTrace(); send(query.user.id, CommonStrings.ERROR_UNKNOWN) } }
    }






    suspend fun <BC : BehaviourContext> BC.requestCompetitionName(user: User, message: AccessibleMessage) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_NAME_REQUESTED
        ).execute()

        reply(message, "Будь ласка, введіть назву конкурсу.")
    }

    data class COMPETITION_VINE_TYPE_REQUESTED_METADATA(
        val name: Competition.Name
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionVineType(user: User, message: AccessibleMessage, name: Competition.Name) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_VINE_TYPE_REQUESTED,
            COMPETITION_VINE_TYPE_REQUESTED_METADATA(name).toConversationMetadata()
        ).execute()

        reply(message, "Будь ласка, оберіть тип вина на конкурсі \"${name.value}\".", replyMarkup = InlineKeyboardMarkup(
            matrix {
                row {
                    +CallbackDataInlineKeyboardButton("🍷 Червоне вино", "red_vine_type")
                    +CallbackDataInlineKeyboardButton("🍾 Біле вино", "white_vine_type")
                }
            })
        )
    }

    data class COMPETITION_EXPERTS_SELECTION_REQUESTED_METADATA(
        val name: Competition.Name,
        val vineType: Vine.Type,
        val selectedExperts: List<User.PhoneNumber> = listOf()
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionExperts(user: User, name: Competition.Name, vineType: Vine.Type) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_EXPERTS_SELECTION_REQUESTED,
            COMPETITION_EXPERTS_SELECTION_REQUESTED_METADATA(name, vineType).toConversationMetadata()
        ).execute()

        val experts = FilterUsersByRoleRequest(User.Role.EXPERT).execute()

        val newUser = user.copy(
            currentInlineMarkupButtons = experts.map { listOf(CallbackDataInlineKeyboardButton(
                        it.name.value,
                        it.id.value + "_expert"
                    )) },
            inlineMarkupPagination = Pagination(0, 12)
        )

        UpdateInlineMarkupStateRequest(
            user.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        ).execute()

        send(
            newUser.chatId!!.toChatId(),
            "Виберіть експертів зі списку нижче. Ви можете вибрати декілька експертів.",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }

    data class COMPETITION_CATEGORIES_SELECTION_REQUESTED_METADATA(
        val name: Competition.Name,
        val vineType: Vine.Type,
        val selectedExperts: List<User.PhoneNumber> = listOf(),
        val selectedCategories: List<Category.Name> = listOf()
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionCategories(user: User, name: Competition.Name, vineType: Vine.Type, selectedExperts: List<User.PhoneNumber>) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_CATEGORIES_SELECTION_REQUESTED,
            COMPETITION_CATEGORIES_SELECTION_REQUESTED_METADATA(name, vineType, selectedExperts, listOf()).toConversationMetadata()
        ).execute()

        val categories = listOf(
            Category(Category.Name("Limpidity")),
            Category(Category.Name("Colour")),
            Category(Category.Name("Aspect other than limpidity")),
            Category(Category.Name("Effervescence")),
            Category(Category.Name("Genuineness (Still wines)")),
            Category(Category.Name("Genuineness (Sparkling wines)")),
            Category(Category.Name("Nose Typicality")),
            Category(Category.Name("Nose Positive intensity (Still wines)")),
            Category(Category.Name("Nose Positive intensity (Sparkling wines)")),
            Category(Category.Name("Nose Positive intensity (Spiritous beverages)")),
            Category(Category.Name("Nose Quality (Still wines)")),
            Category(Category.Name("Nose Quality (Sparkling wines)")),
            Category(Category.Name("Nose Quality (Spiritous beverages)")),
            Category(Category.Name("Taste Typicality")),
            Category(Category.Name("Taste Positive intensity (Still wines)")),
            Category(Category.Name("Taste Positive intensity (Sparkling wines)")),
            Category(Category.Name("Harmonious persistence (Still wines)")),
            Category(Category.Name("Harmonious persistence (Sparkling wines)")),
            Category(Category.Name("Harmonious persistence (Spiritous beverages)")),
            Category(Category.Name("Taste Quality (Still wines)")),
            Category(Category.Name("Taste Quality (Sparkling wines)")),
            Category(Category.Name("Taste Quality (Spiritous beverages)")),
            Category(Category.Name("Overall judgement (Still wines)")),
            Category(Category.Name("Overall judgement (Sparkling wines)")),
            Category(Category.Name("Overall judgement (Spiritous beverages)")),
        )

        val newUser = user.copy(
            currentInlineMarkupButtons = categories.map { listOf(CallbackDataInlineKeyboardButton(
                it.name.value,
                it.name.value + "_category"
            )) },
            inlineMarkupPagination = Pagination(0, 12)
        )

        UpdateInlineMarkupStateRequest(
            user.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        ).execute()

        send(
            newUser.chatId!!.toChatId(),
            "Виберіть категорії зі списку нижче. Ви можете вибрати декілька категорій.",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }

    data class COMPETITION_VINES_REQUESTED_METADATA(
        val name: Competition.Name,
        val vineType: Vine.Type,
        val selectedExperts: List<User.PhoneNumber> = listOf(),
        val selectedCategories: List<Category.Name> = listOf(),
        val selectedVines: List<Vine> = listOf()
    )
    suspend fun <BC : BehaviourContext> BC.requestCompetitionVines(user: User, name: Competition.Name, vineType: Vine.Type, selectedExperts: List<User.PhoneNumber>, selectedCategories: List<Category.Name>) {
        UpdateConversationStateRequest(
            user.chatId!!,
            ConversationState.COMPETITION_VINES_REQUESTED,
            COMPETITION_VINES_REQUESTED_METADATA(name, vineType, selectedExperts, selectedCategories, listOf()).toConversationMetadata()
        ).execute()

        UpdateInlineMarkupStateRequest(
            user.chatId,
            emptyList(),
            Pagination(0, 12)
        ).execute()

        send(
            user.chatId.toChatId(),
            "Введіть вина, які будуть оцінюватися на конкурсі у форматі \"<Sample Code> : <Vine Type>\" (без кавичок). Vine Type може бути \"STILL\", \"SPARKLING\" та \"SPIRITOUS\". Можна вводити декілька вин за один раз -- кожне в новому рядку.\n" +
                    "Наприклад:\n" +
                    "Sample Code 1 : STILL\n" +
                    "Sample Code 2 : SPARKLING\n" +
                    "Sample Code 3 : SPIRITOUS\n",
            replyMarkup = MenuUtils.EMPTY_INLINE_MENU
        )
    }
    suspend fun <BC : BehaviourContext> BC.addCompetitionVines(
        user: User,
        message: AccessibleMessage,
        metadata: COMPETITION_VINES_REQUESTED_METADATA,
    ) {
        val wasEmpty = metadata.selectedVines.isEmpty()
        val unprocessedVines = mutableListOf<String>()
        val newVines = message.text!!.split("\n").mapNotNull { vineString ->
            if (vineString.isBlank()) return@mapNotNull null
            val parts = vineString.split(":").map { it.trim() }
            if (parts.size != 2) {
                unprocessedVines.add("Неправильний формат -- $vineString")
                return@mapNotNull null
            }
            return@mapNotNull try {
//                val phoneNumber = GetUserByNameAndRoleRequest(
//                    User.Name(parts[0]),
//                    User.Role.VINE_MAKER
//                ).execute()?.phoneNumber ?: run {
//                    unprocessedVines.add("Не знайдено винороба з іменем ${parts[0]} -- $vineString")
//                    return@mapNotNull null
//                }
                val newVine = Vine(null, null, metadata.vineType, Vine.SampleCode(parts[0]), Vine.RealType.fromString(parts[1]) ?: run {
                    unprocessedVines.add("Неправильний тип вина (${parts[1]}) -- $vineString")
                    return@mapNotNull null
                })
                if(metadata.selectedVines.contains(newVine)) {
                    unprocessedVines.add("Це вино вже додано -- $vineString")
                    return@mapNotNull null
                } else newVine
            } catch (e: Exception) {
                unprocessedVines.add("Невідома помилка при обробці вина (можливо надто довга назва) -- $vineString")
                null
            }
        }

        val newMetadata = metadata.copy(selectedVines = (metadata.selectedVines + newVines).distinctBy { it })
        val newUser = user.copy(
            conversationMetadata = newMetadata.toConversationMetadata(),
            currentInlineMarkupButtons = (user.currentInlineMarkupButtons + newVines.map { listOf(CallbackDataInlineKeyboardButton(
                it.sampleCode.value,
                it.id.value + "_vine"
            )) }).map { row ->
                row.distinctBy { it.callbackData.split("_").first() }
            }.let {
                if(wasEmpty && newMetadata.selectedVines.isNotEmpty()) it + listOf(
                    listOf(CallbackDataInlineKeyboardButton("✅ Завершити вибір вин", "(always_on)finish_vines_selection"))
                ).toSet() else it
            },
            inlineMarkupPagination = user.inlineMarkupPagination
        )

        UpdateConversationStateRequest(
            newUser.chatId!!,
            ConversationState.COMPETITION_VINES_REQUESTED,
            newMetadata.toConversationMetadata()
        ).execute()

        UpdateInlineMarkupStateRequest(
            newUser.chatId,
            newUser.currentInlineMarkupButtons,
            newUser.inlineMarkupPagination
        ).execute()

        reply(message,
            (if(unprocessedVines.isNotEmpty()) "Наступні вина не були додані:\n" +
                unprocessedVines.joinToString("\n") + "\n" else "") +
                "Нижче розміщені всі успішно додані вина. Ви можете видалити вино, натиснувши на нього. Якщо ви хочете завершити вибір вин, надішліть \"✅ Завершити вибір вин\".",
            replyMarkup = InlineMarkupPaginationUtils.generateInlineMarkup(newUser)
        )
    }

    suspend fun <BC : BehaviourContext> BC.createCompetition(
        user: User,
        name: Competition.Name,
        vineType: Vine.Type,
        selectedExperts: List<User>,
        selectedCategories: List<Category>,
        selectedVines: List<Vine>
    ) {
        val activeCompetition = GetActiveCompetitionRequest().execute()
        if(activeCompetition != null) {
            send(user.chatId!!.toChatId(), "Наразі вже триває конкурс: ${activeCompetition.name.value}. Будь ласка, зачекайте, поки він завершиться.")
            return
        }


        val competition = Competition(
            id = Competition.Id(),
            name = name,
            vineType = vineType,
            startedAt = Timestamp.now(),
            endedAt = null,
            experts = selectedExperts,
            categories = selectedCategories,
            vines = selectedVines
        )
        val competitionRepository: CompetitionRepository by inject(CompetitionRepository::class.java)
        competitionRepository.create(competition)

        UpdateConversationStateRequest(user.chatId!!, ConversationState.INITIAL).execute()

        send(user.chatId.toChatId(), "Конкурс ${competition.name.value} успішно створено!")

        newSuspendedTransaction {
            UsersTable.selectAll().forEach { row ->
                try {
                    tryWithRetry(3, 1000) {
                        val rowUser = UsersTable.fromRow(row)
                        if(rowUser.chatId != null) {
                            UpdateConversationStateRequest(rowUser.chatId, ConversationState.INITIAL).execute()
                            send(rowUser.chatId.toChatId(), "Конкурс ${competition.name.value} вже розпочато!", replyMarkup = MenuUtils.generateMenu(ConversationState.INITIAL, rowUser.role, true))
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to send competition started message to user $row" }
                }
            }
        }
    }
}