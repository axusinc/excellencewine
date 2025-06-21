package domain.model.entity

import eth.likespro.commons.models.Entity
import eth.likespro.commons.models.Validatable
import eth.likespro.commons.models.Value
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import kotlinx.serialization.Serializable

@Serializable
data class VineAssessment(
    val from: User.PhoneNumber,
    val to: Vine.Id,
    val category: Category.Name,
    val mark: Int
) : Entity<VineAssessment.Id>, Validatable<VineAssessment> {
    override val id: Id
        get() = Id("${from.value}_${to.encodeObject()}_${category.value}")

    @Serializable
    data class Id(
        val value: String
    ) : Value, Validatable<Id> {
        class IsInvalidException(override val message: String) : RuntimeException()

        init {
            throwIfInvalid()
        }

        override fun throwIfInvalid(): Id = this.also {
            if (value.isBlank()) {
                throw IsInvalidException("Vine ID cannot be blank")
            }
        }
    }

    class IsInvalidException(override val message: String) : RuntimeException()

    override fun throwIfInvalid(): VineAssessment = this.also {
        if (mark < 1 || mark > 5) {
            throw IsInvalidException("Mark must be between 1 and 5")
        }
    }
}