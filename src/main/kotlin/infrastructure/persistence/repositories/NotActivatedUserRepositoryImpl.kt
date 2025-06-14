package infrastructure.persistence.repositories

import domain.model.entity.NotActivatedUser
import domain.model.entity.User
import domain.model.value.PhoneNumber
import domain.ports.repositories.NotActivatedUserRepository
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.adapters.AtomarixExposedAdapter
import eth.likespro.commons.models.Pagination
import infrastructure.persistence.tables.NotActivatedUsersTable
import infrastructure.persistence.tables.UsersTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class NotActivatedUserRepositoryImpl: NotActivatedUserRepository {
    init {
        transaction {
            SchemaUtils.create(NotActivatedUsersTable)
        }
    }


    override suspend fun findById(
        atom: Atom,
        id: PhoneNumber
    ): NotActivatedUser? = AtomarixExposedAdapter.runWithAdapter(atom) {
        NotActivatedUsersTable.selectAll()
            .where { NotActivatedUsersTable.phoneNumber eq id.value }
            .map { NotActivatedUsersTable.fromRow(it) }
            .firstOrNull()
    }

    override suspend fun isExisting(
        atom: Atom,
        id: PhoneNumber
    ): Boolean = AtomarixExposedAdapter.runWithAdapter(atom) {
        NotActivatedUsersTable.selectAll()
            .where { NotActivatedUsersTable.phoneNumber eq id.value }
            .empty().not()
    }

    override suspend fun findAll(atom: Atom): List<NotActivatedUser> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(
        atom: Atom,
        pagination: Pagination
    ): List<NotActivatedUser> {
        TODO("Not yet implemented")
    }

    override suspend fun create(
        atom: Atom,
        entity: NotActivatedUser
    ): NotActivatedUser = AtomarixExposedAdapter.runWithAdapter(atom) {
        NotActivatedUsersTable.insert {
            NotActivatedUsersTable.copy(entity, it)
        }.let { entity }
    }

    override suspend fun upsert(
        atom: Atom,
        entity: NotActivatedUser
    ): NotActivatedUser {
        TODO("Not yet implemented")
    }

    override suspend fun update(
        atom: Atom,
        entity: NotActivatedUser
    ): NotActivatedUser? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(atom: Atom, id: PhoneNumber) {
        TODO("Not yet implemented")
    }

    override suspend fun count(
        atom: Atom,
        pagination: Pagination?
    ): Long {
        TODO("Not yet implemented")
    }

    override suspend fun filterByRole(
        atom: Atom,
        role: User.Role
    ): List<NotActivatedUser> = AtomarixExposedAdapter.runWithAdapter(atom) {
        NotActivatedUsersTable.selectAll()
            .where { NotActivatedUsersTable.role eq role }
            .map { NotActivatedUsersTable.fromRow(it) }
    }
}