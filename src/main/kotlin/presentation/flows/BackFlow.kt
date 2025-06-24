package presentation.flows

import application.usecase.GetUserByChatIdRequest
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import domain.model.entity.User
import domain.model.value.ConversationState
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import presentation.CommonStrings
import presentation.flows.AddUserFlow.displayUsersList
import presentation.flows.AddUserFlow.requestUserPhoneNumber
import presentation.flows.AssessFlow.requestCompetitionCategoryPick
import presentation.flows.AssessFlow.requestCompetitionVinePick
import presentation.flows.CreateCompetitionFlow.requestCompetitionCategories
import presentation.flows.CreateCompetitionFlow.requestCompetitionExperts
import presentation.flows.CreateCompetitionFlow.requestCompetitionName
import presentation.flows.CreateCompetitionFlow.requestCompetitionVineType
import presentation.flows.MainScreenFlow.showMainScreen

object BackFlow {
    suspend fun <BC : BehaviourContext> BC.setupBackButtonFlow() {
        onText { message ->
            if(!message.content.text.equals(CommonStrings.BACK_BUTTON_TEXT, ignoreCase = true))
                return@onText

            val user = GetUserByChatIdRequest(message.chat.id.chatId).execute()!!
            moveBack(user, message)
        }
    }

    suspend fun <BC : BehaviourContext> BC.moveBack(user: User, message: AccessibleMessage) { when(user.conversationState) {
        ConversationState.USERS_LIST_DISPLAYED -> showMainScreen(user, message)
        ConversationState.USER_PHONE_NUMBER_REQUESTED -> {
            val metadata = user.conversationMetadata.value.decodeObject<AddUserFlow.USER_PHONE_NUMBER_REQUESTED_METADATA>()
            displayUsersList(user, message, metadata.withRole)
        }
        ConversationState.USER_NAME_REQUESTED -> {
            val metadata = user.conversationMetadata.value.decodeObject<AddUserFlow.USER_NAME_REQUESTED_METADATA>()
            requestUserPhoneNumber(user, metadata.withRole)
        }

        ConversationState.COMPETITIONS_LIST_DISPLAYED -> showMainScreen(user, message)

        ConversationState.COMPETITION_NAME_REQUESTED -> showMainScreen(user, message)
        ConversationState.COMPETITION_VINE_TYPE_REQUESTED -> requestCompetitionName(user, message)
        ConversationState.COMPETITION_EXPERTS_SELECTION_REQUESTED -> {
            val metadata = user.conversationMetadata.value.decodeObject<CreateCompetitionFlow.COMPETITION_EXPERTS_SELECTION_REQUESTED_METADATA>()
            requestCompetitionVineType(user, message, metadata.name)
        }
        ConversationState.COMPETITION_CATEGORIES_SELECTION_REQUESTED -> {
            val metadata = user.conversationMetadata.value.decodeObject<CreateCompetitionFlow.COMPETITION_CATEGORIES_SELECTION_REQUESTED_METADATA>()
            requestCompetitionExperts(user, metadata.name, metadata.vineType)
        }
        ConversationState.COMPETITION_VINES_REQUESTED -> {
            val metadata = user.conversationMetadata.value.decodeObject<CreateCompetitionFlow.COMPETITION_VINES_REQUESTED_METADATA>()
//            requestCompetitionCategories(user, metadata.name, metadata.vineType, metadata.selectedExperts)
            requestCompetitionExperts(user, metadata.name, metadata.vineType)
        }

        ConversationState.COMPETITION_VINE_PICK_REQUESTED -> showMainScreen(user, message)
        ConversationState.COMPETITION_CATEGORY_PICK_REQUESTED -> requestCompetitionVinePick(user, message)
        ConversationState.COMPETITION_VINE_MARK_REQUESTED -> {
            val metadata = user.conversationMetadata.value.decodeObject<AssessFlow.COMPETITION_VINE_MARK_REQUESTED_METADATA>()
            requestCompetitionCategoryPick(user, metadata.vineId)
        }

        ConversationState.END_COMPETITION_REQUESTED -> showMainScreen(user, message)

        else -> reply(message, "Неможливо повернутися назад з цього стану.")
    } }
}