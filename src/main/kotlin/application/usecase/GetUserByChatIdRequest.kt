package application.usecase

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import domain.model.entity.User
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

@Serializable
data class GetUserByChatIdRequest(
    val chatId: RawChatId
) {
    suspend fun execute(): User? {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        return userRepository.findByChatId(chatId)
    }
}