package infrastructure.persistence.tables

import dev.inmo.tgbotapi.types.RawChatId
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

object UsersTable: Table("users") {
    val phoneNumber = varchar("phone_number", 20).uniqueIndex()
    val chatId = long("chat_id").uniqueIndex().nullable()
    val name = varchar("name", 28)
    val role = enumeration("role", User.Role::class)
    val conversationState = enumeration("conversation_state", ConversationState::class)
    val conversationMetadata = text("conversation_metadata")
    val currentInlineMarkupButtons = text("current_inline_markup_buttons")
    val inlineMarkupPagination = text("inline_markup_pagination")

    override val primaryKey = PrimaryKey(phoneNumber)

    fun fromRow(row: ResultRow): User = User(
        phoneNumber = User.PhoneNumber(row[phoneNumber]),
        chatId = row[chatId]?.let { RawChatId(it) },
        name = User.Name(row[name]),
        role = row[role],
        conversationState = row[conversationState],
        conversationMetadata = ConversationMetadata(row[conversationMetadata]),
        currentInlineMarkupButtons = row[currentInlineMarkupButtons].decodeObject(),
        inlineMarkupPagination = row[inlineMarkupPagination].decodeObject()
    )

    fun copy(user: User, to: UpdateBuilder<Int>) {
        to[phoneNumber] = user.phoneNumber.value
        to[chatId] = user.chatId?.long
        to[name] = user.name.value
        to[role] = user.role
        to[conversationState] = user.conversationState
        to[conversationMetadata] = user.conversationMetadata.value
        to[currentInlineMarkupButtons] = user.currentInlineMarkupButtons.encodeObject()
        to[inlineMarkupPagination] = user.inlineMarkupPagination.encodeObject()
    }
}