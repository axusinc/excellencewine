package application.usecase

import dev.inmo.tgbotapi.types.RawChatId
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class UpdateConversationStateRequest(
    val chatId: RawChatId,
    val conversationState: ConversationState,
    val conversationMetadata: ConversationMetadata = ConversationMetadata("")
) {
    suspend fun execute() {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        userRepository.updateConversationState(chatId, conversationState, conversationMetadata)
    }
}