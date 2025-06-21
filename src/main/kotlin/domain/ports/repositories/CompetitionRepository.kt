package domain.ports.repositories

import domain.model.entity.Competition
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.Atom.Companion.atomic
import eth.likespro.atomarix.AtomarixRepository

interface CompetitionRepository: AtomarixRepository<Competition, Competition.Id> {
    suspend fun findByName(name: Competition.Name): Competition? = atomic { findByName(this, name) }
    suspend fun findByName(atom: Atom, name: Competition.Name): Competition?

    suspend fun findActive(): Competition? = atomic { findActive(this) }
    suspend fun findActive(atom: Atom): Competition?
}