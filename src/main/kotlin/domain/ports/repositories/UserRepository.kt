package domain.ports.repositories

import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import domain.model.value.PhoneNumber
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.Atom.Companion.atomic
import eth.likespro.atomarix.AtomarixRepository

interface UserRepository: AtomarixRepository<User, User.Id> {
    suspend fun filterByRole(role: User.Role): List<User> = atomic { filterByRole(this, role) }
    suspend fun filterByRole(atom: Atom, role: User.Role): List<User>

    suspend fun updateConversationState(id: User.Id, conversationState: ConversationState, conversationMetadata: ConversationMetadata) = atomic { updateConversationState(this, id, conversationState, conversationMetadata) }
    suspend fun updateConversationState(atom: Atom, id: User.Id, conversationState: ConversationState, conversationMetadata: ConversationMetadata)

    suspend fun isExistingByPhoneNumber(phoneNumber: PhoneNumber): Boolean = atomic { isExistingByPhoneNumber(this, phoneNumber) }
    suspend fun isExistingByPhoneNumber(atom: Atom, phoneNumber: PhoneNumber): Boolean
}