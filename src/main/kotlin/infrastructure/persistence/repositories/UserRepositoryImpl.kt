package infrastructure.persistence.repositories

import domain.model.entity.User
import domain.model.value.ConversationMetadata
import domain.model.value.ConversationState
import domain.model.value.PhoneNumber
import domain.ports.repositories.UserRepository
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.adapters.AtomarixExposedAdapter
import eth.likespro.commons.models.Pagination
import infrastructure.persistence.tables.UsersTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserRepositoryImpl: UserRepository {
    init {
        transaction {
            SchemaUtils.create(UsersTable)
        }
    }

    override suspend fun findById(
        atom: Atom,
        id: User.Id
    ): User? = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { UsersTable.id eq id.value }
            .singleOrNull()
            ?.let { UsersTable.fromRow(it) }
    }

    override suspend fun isExisting(atom: Atom, id: User.Id): Boolean = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { UsersTable.id eq id.value }
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
    ): User? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(atom: Atom, id: User.Id) {
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

    override suspend fun updateConversationState(
        atom: Atom,
        id: User.Id,
        conversationState: ConversationState,
        conversationMetadata: ConversationMetadata
    ) = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.update({ UsersTable.id eq id.value }) {
            it[UsersTable.conversationState] = conversationState
            it[UsersTable.conversationMetadata] = conversationMetadata.value
        }.let {  }
    }

    override suspend fun isExistingByPhoneNumber(atom: Atom, phoneNumber: PhoneNumber): Boolean = AtomarixExposedAdapter.runWithAdapter(atom) {
        UsersTable.selectAll()
            .where { UsersTable.phoneNumber eq phoneNumber.value }
            .empty().not()
    }
}