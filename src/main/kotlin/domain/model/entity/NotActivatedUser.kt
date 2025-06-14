package domain.model.entity

import domain.model.value.PhoneNumber
import eth.likespro.commons.models.Entity
import kotlinx.serialization.Serializable

@Serializable
data class NotActivatedUser(
    val phoneNumber: PhoneNumber,
    val name: User.Name,
    val role: User.Role
): Entity<PhoneNumber> {
    override val id: PhoneNumber
        get() = phoneNumber
}