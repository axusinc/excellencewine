package domain.ports.repositories

import domain.model.entity.*
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.Atom.Companion.atomic
import eth.likespro.atomarix.AtomarixRepository

interface VineAssessmentRepository: AtomarixRepository<VineAssessment, VineAssessment.Id> {
    suspend fun filter(competitionId: Competition.Id? = null, from: User.PhoneNumber? = null, to: Vine.SampleCode? = null, category: Category.Name? = null): List<VineAssessment> = atomic { filter(this, competitionId, from, to, category) }
    suspend fun filter(atom: Atom, competitionId: Competition.Id? = null, from: User.PhoneNumber? = null, to: Vine.SampleCode? = null, category: Category.Name? = null): List<VineAssessment>
}