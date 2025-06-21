package infrastructure.persistence.repositories

import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import domain.ports.repositories.UserRepository
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.adapters.AtomarixExposedAdapter
import eth.likespro.commons.models.Pagination
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import infrastructure.persistence.tables.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepositoryImpl: UserRepository {
    init {
        transaction {
            SchemaUtils.create(UsersTable)
        }
    }

    override suspend fun findById(
        atom: Atom,
        id: User.PhoneNumber
    ): User? = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { UsersTable.phoneNumber eq id.value }
            .singleOrNull()
            ?.let { UsersTable.fromRow(it) }
    }

    override suspend fun isExisting(atom: Atom, id: User.PhoneNumber): Boolean = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { UsersTable.phoneNumber eq id.value }
            .empty().not()
    }

    override suspend fun findAll(atom: Atom): List<User> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(
        atom: Atom,
        pagination: Pagination
    ): List<User> {
        TODO("Not yet implemented")
    }

    override suspend fun create(
        atom: Atom,
        entity: User
    ): User = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.insert {
            UsersTable.copy(entity, it)
        }.let { entity }
    }

    override suspend fun upsert(
        atom: Atom,
        entity: User
    ): User {
        TODO("Not yet implemented")
    }

    override suspend fun update(
        atom: Atom,
        entity: User
    ): User? = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.update({ UsersTable.phoneNumber eq entity.phoneNumber.value }) {
            UsersTable.copy(entity, it)
        }.let { if (it == 0) null else entity }
    }

    override suspend fun delete(atom: Atom, id: User.PhoneNumber) {
        TODO("Not yet implemented")
    }

    override suspend fun count(
        atom: Atom,
        pagination: Pagination?
    ): Long = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .let { if (pagination != null) it.limit(pagination.itemsPerPage).offset(pagination.offset) else it }
            .count()
    }

    override suspend fun filterByRole(
        atom: Atom,
        role: User.Role
    ): List<User> = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { UsersTable.role eq role }
            .map { UsersTable.fromRow(it) }
    }

    override suspend fun findByChatId(
        atom: Atom,
        chatId: RawChatId
    ): User? = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { UsersTable.chatId eq chatId.long }
            .singleOrNull()
            ?.let { UsersTable.fromRow(it) }
    }

    override suspend fun findByNameAndRole(atom: Atom, name: User.Name, role: User.Role): User? = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { (UsersTable.name eq name.value) and (UsersTable.role eq role) }
            .singleOrNull()
            ?.let { UsersTable.fromRow(it) }
    }

    override suspend fun updateConversationState(
        atom: Atom,
        chatId: RawChatId,
        conversationState: ConversationState,
        conversationMetadata: ConversationMetadata
    ) = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.update({ UsersTable.chatId eq chatId.long }) {
            it[UsersTable.conversationState] = conversationState
            it[UsersTable.conversationMetadata] = conversationMetadata.value
        }.let {  }
    }

    override suspend fun updateInlineMarkupState(
        atom: Atom,
        chatId: RawChatId,
        currentInlineMarkupButtons: List<List<CallbackDataInlineKeyboardButton>>,
        inlineMarkupPagination: Pagination
    ) = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.update({ UsersTable.chatId eq chatId.long }) {
            it[UsersTable.currentInlineMarkupButtons] = currentInlineMarkupButtons.encodeObject()
            it[UsersTable.inlineMarkupPagination] = inlineMarkupPagination.encodeObject()
        }.let {  }
    }
}