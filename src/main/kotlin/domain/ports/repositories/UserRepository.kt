package domain.ports.repositories

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.Atom.Companion.atomic
import eth.likespro.atomarix.AtomarixRepository
import eth.likespro.commons.models.Pagination

interface UserRepository: AtomarixRepository<User, User.PhoneNumber> {
    suspend fun filterByRole(role: User.Role): List<User> = atomic { filterByRole(this, role) }
    suspend fun filterByRole(atom: Atom, role: User.Role): List<User>

    suspend fun findByChatId(chatId: RawChatId): User? = atomic { findByChatId(this, chatId) }
    suspend fun findByChatId(atom: Atom, chatId: RawChatId): User?

    suspend fun findByNameAndRole(name: User.Name, role: User.Role): User? = atomic { findByNameAndRole(this, name, role) }
    suspend fun findByNameAndRole(atom: Atom, name: User.Name, role: User.Role): User?

    suspend fun updateConversationState(chatId: RawChatId, conversationState: ConversationState, conversationMetadata: ConversationMetadata) = atomic { updateConversationState(this, chatId, conversationState, conversationMetadata) }
    suspend fun updateConversationState(atom: Atom, chatId: RawChatId, conversationState: ConversationState, conversationMetadata: ConversationMetadata)

    suspend fun updateInlineMarkupState(
        chatId: RawChatId,
        currentInlineMarkupButtons: List<List<CallbackDataInlineKeyboardButton>>,
        inlineMarkupPagination: Pagination = Pagination.ALL
    ) = atomic { updateInlineMarkupState(this, chatId, currentInlineMarkupButtons, inlineMarkupPagination) }
    suspend fun updateInlineMarkupState(atom: Atom, chatId: RawChatId, currentInlineMarkupButtons: List<List<CallbackDataInlineKeyboardButton>>, inlineMarkupPagination: Pagination = Pagination.ALL)
}