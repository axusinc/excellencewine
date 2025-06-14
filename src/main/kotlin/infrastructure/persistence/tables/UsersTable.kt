package infrastructure.persistence.tables

import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import domain.model.value.PhoneNumber
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

object UsersTable: Table("users") {
    val id = long("id").uniqueIndex()
    val name = varchar("name", 28)
    val role = enumeration("role", User.Role::class)
    val phoneNumber = varchar("phone_number", 20)
    val conversationState = enumeration("conversation_state", ConversationState::class)
    val conversationMetadata = text("conversation_metadata")

    override val primaryKey = PrimaryKey(id)

    fun fromRow(row: ResultRow): User = User(
        id = User.Id(row[id]),
        name = User.Name(row[name]),
        role = row[role],
        phoneNumber = PhoneNumber(row[phoneNumber]),
        conversationState = row[conversationState],
        conversationMetadata = ConversationMetadata(row[conversationMetadata])
    )

    fun copy(user: User, to: UpdateBuilder<Int>) {
        to[id] = user.id.value
        to[name] = user.name.value
        to[role] = user.role
        to[phoneNumber] = user.phoneNumber.value
        to[conversationState] = user.conversationState
        to[conversationMetadata] = user.conversationMetadata.value
    }
}