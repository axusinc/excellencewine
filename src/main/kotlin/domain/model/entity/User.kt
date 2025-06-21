package domain.model.entity

import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import eth.likespro.commons.models.Entity
import eth.likespro.commons.models.Pagination
import eth.likespro.commons.models.Validatable
import eth.likespro.commons.models.Value
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val phoneNumber: PhoneNumber,
    val chatId: RawChatId?, // Null if the user has not started the bot yet
    val name: Name,
    val role: Role,
    val conversationState: ConversationState = ConversationState.INITIAL,
    val conversationMetadata: ConversationMetadata = ConversationMetadata(""),
    val currentInlineMarkupButtons: List<List<CallbackDataInlineKeyboardButton>> = emptyList(),
    val inlineMarkupPagination: Pagination = Pagination.ALL,
): Entity<User.PhoneNumber> {
    override val id: PhoneNumber
        get() = phoneNumber

    @Serializable
    data class PhoneNumber(
        val value: String
    ): Value, Validatable<PhoneNumber> {
        class IsInvalidException(override val message: String): Exception()

        init {
            throwIfInvalid()
        }

        override fun throwIfInvalid(): PhoneNumber = this.also {
            if(!value.startsWith("+"))
                throw IsInvalidException("Phone number must start with '+' sign")
            if(value.drop(1).any { !it.isDigit() })
                throw IsInvalidException("Phone number must contain only digits after the '+' sign")
        }
    }

    @Serializable
    data class Name(
        val value: String
    ): Value, Validatable<Name> {
        class IsInvalidException(override val message: String): Exception()

        init {
            throwIfInvalid()
        }

        override fun throwIfInvalid(): Name = this.also {
            if (value.isBlank())
                throw IsInvalidException("Name cannot be blank")
            if (value.length > 28)
                throw IsInvalidException("Name cannot be longer than 28 characters")
        }
    }

    enum class Role {
        ADMIN,
        HEAD_OF_EXPERTS,
        EXPERT,
        VINE_MAKER
    }
}