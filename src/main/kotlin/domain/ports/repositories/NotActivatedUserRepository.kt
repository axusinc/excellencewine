package domain.ports.repositories

import domain.model.entity.NotActivatedUser
import domain.model.entity.User
import domain.model.value.PhoneNumber
import eth.likespro.atomarix.Atom
import eth.likespro.atomarix.Atom.Companion.atomic
import eth.likespro.atomarix.AtomarixRepository

interface NotActivatedUserRepository: AtomarixRepository<NotActivatedUser, PhoneNumber> {
    suspend fun filterByRole(role: User.Role): List<NotActivatedUser> = atomic { filterByRole(this, role) }
    suspend fun filterByRole(atom: Atom, role: User.Role): List<NotActivatedUser>
}