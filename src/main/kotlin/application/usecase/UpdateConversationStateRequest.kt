package application.usecase

import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class UpdateConversationStateRequest(
    val id: User.Id,
    val conversationState: ConversationState,
    val conversationMetadata: ConversationMetadata = ConversationMetadata("")
) {
    suspend fun execute() {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        userRepository.updateConversationState(id, conversationState, conversationMetadata)
    }
}