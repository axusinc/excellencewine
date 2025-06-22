package infrastructure.persistence.repositories

import domain.model.entity.*
import domain.ports.repositories.VineAssessmentRepository
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.adapters.AtomarixExposedAdapter
import eth.likespro.commons.models.Pagination
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import infrastructure.persistence.tables.VineAssessmentsTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

class VineAssessmentRepositoryImpl: VineAssessmentRepository {
    init {
        transaction {
            SchemaUtils.create(VineAssessmentsTable)
        }
    }

    override suspend fun filter(
        atom: Atom,
        competitionId: Competition.Id?,
        from: User.PhoneNumber?,
        to: Vine.SampleCode?,
        category: Category.Name?
    ): List<VineAssessment> = AtomarixExposedAdapter.runWithAdapter(atom) {
        VineAssessmentsTable.selectAll()
            .let { if(competitionId != null) it.andWhere { VineAssessmentsTable.competitionId eq competitionId.value } else it }
            .let { if(from != null) it.andWhere { VineAssessmentsTable.from eq from.value } else it }
            .let { if(to != null) it.andWhere { VineAssessmentsTable.to eq to.encodeObject() } else it }
            .let { if(category != null) it.andWhere { VineAssessmentsTable.category eq category.value } else it }
            .map { VineAssessmentsTable.fromRow(it) }
    }

    override suspend fun count(atom: Atom, pagination: Pagination?): Long {
        TODO("Not yet implemented")
    }

    override suspend fun create(atom: Atom, entity: VineAssessment): VineAssessment {
        TODO("Not yet implemented")
    }

    override suspend fun delete(atom: Atom, id: VineAssessment.Id) {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(atom: Atom, pagination: Pagination): List<VineAssessment> {
        TODO("Not yet implemented")
    }

    override suspend fun findById(atom: Atom, id: VineAssessment.Id): VineAssessment? {
        TODO("Not yet implemented")
    }

    override suspend fun isExisting(atom: Atom, id: VineAssessment.Id): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun update(atom: Atom, entity: VineAssessment): VineAssessment? {
        TODO("Not yet implemented")
    }

    override suspend fun upsert(atom: Atom, entity: VineAssessment): VineAssessment = AtomarixExposedAdapter.runWithAdapter(atom) {
        VineAssessmentsTable.upsert (VineAssessmentsTable.id) {
            VineAssessmentsTable.copy(entity, it)
        }.let { entity }
    }

}