package application.usecase

import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import domain.ports.repositories.UserRepository
import eth.likespro.commons.models.Pagination
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class UpdateInlineMarkupStateRequest(
    val chatId: RawChatId,
    val currentInlineMarkupButtons: List<List<CallbackDataInlineKeyboardButton>>,
    val inlineMarkupPagination: Pagination
) {
    suspend fun execute() {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        userRepository.updateInlineMarkupState(chatId, currentInlineMarkupButtons, inlineMarkupPagination)
    }
}