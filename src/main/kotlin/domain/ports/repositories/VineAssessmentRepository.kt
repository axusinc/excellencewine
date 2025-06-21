package domain.ports.repositories

import domain.model.entity.Category
import domain.model.entity.User
import domain.model.entity.Vine
import domain.model.entity.VineAssessment
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.Atom.Companion.atomic
import eth.likespro.atomarix.AtomarixRepository

interface VineAssessmentRepository: AtomarixRepository<VineAssessment, VineAssessment.Id> {
    suspend fun filter(from: User.PhoneNumber?, to: Vine.Id?, category: Category.Name?): List<VineAssessment> = atomic { filter(this, from, to, category) }
    suspend fun filter(atom: Atom, from: User.PhoneNumber?, to: Vine.Id?, category: Category.Name?): List<VineAssessment>
}