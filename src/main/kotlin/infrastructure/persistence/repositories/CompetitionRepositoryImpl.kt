package infrastructure.persistence.repositories

import domain.model.entity.Competition
import domain.ports.repositories.CompetitionRepository
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.adapters.AtomarixExposedAdapter
import eth.likespro.commons.models.Pagination
import infrastructure.persistence.tables.CompetitionsTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class CompetitionRepositoryImpl: CompetitionRepository {
    init {
        transaction {
            SchemaUtils.create(CompetitionsTable)
        }
    }

    override suspend fun findByName(atom: Atom, name: Competition.Name): Competition? = AtomarixExposedAdapter.runWithAdapter(atom) {
        CompetitionsTable.selectAll()
            .where { CompetitionsTable.name eq name.value }
            .singleOrNull()
            ?.let { CompetitionsTable.fromRow(it) }
    }

    override suspend fun findActive(atom: Atom): Competition? = AtomarixExposedAdapter.runWithAdapter(atom) {
        CompetitionsTable.selectAll()
            .where { CompetitionsTable.endedAt eq null }
            .singleOrNull()
            ?.let { CompetitionsTable.fromRow(it) }
    }

    override suspend fun count(atom: Atom, pagination: Pagination?): Long {
        TODO("Not yet implemented")
    }

    override suspend fun create(atom: Atom, entity: Competition): Competition = AtomarixExposedAdapter.runWithAdapter(atom) {
        CompetitionsTable.insert {
            CompetitionsTable.copy(entity, it)
        }.let { entity }
    }

    override suspend fun delete(atom: Atom, id: Competition.Id) {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(atom: Atom, pagination: Pagination): List<Competition> = AtomarixExposedAdapter.runWithAdapter(atom) {
        CompetitionsTable.selectAll()
            .limit(pagination.itemsPerPage).offset(pagination.offset)
            .map { CompetitionsTable.fromRow(it) }
    }

    override suspend fun findById(atom: Atom, id: Competition.Id): Competition? = AtomarixExposedAdapter.runWithAdapter(atom) {
        CompetitionsTable.selectAll()
            .where { CompetitionsTable.id eq id.value }
            .singleOrNull()
            ?.let { CompetitionsTable.fromRow(it) }
    }

    override suspend fun isExisting(atom: Atom, id: Competition.Id): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun update(atom: Atom, entity: Competition): Competition? = AtomarixExposedAdapter.runWithAdapter(atom) {
        CompetitionsTable.update({ CompetitionsTable.id eq entity.id.value }) {
            CompetitionsTable.copy(entity, it)
        }.let { if (it > 0) entity else null }
    }

    override suspend fun upsert(atom: Atom, entity: Competition): Competition {
        TODO("Not yet implemented")
    }
}