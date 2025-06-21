package domain.model.entity

import eth.likespro.commons.models.Entity
import eth.likespro.commons.models.Validatable
import eth.likespro.commons.models.Value
import kotlinx.serialization.Serializable

@Serializable
data class Vine(
    val makerPhoneNumber: User.PhoneNumber,
    val name: Name,
    val type: Type,
): Entity<Vine.Id> {
    override val id: Id
        get() = Id(
            makerPhoneNumber = makerPhoneNumber,
            name = name
        )

    @Serializable
    data class Id(
        val makerPhoneNumber: User.PhoneNumber,
        val name: Name
    ): Value {
        constructor(id: String): this(User.PhoneNumber(id.split(":")[0]), Name(id.split(":")[1]))
        override fun toString(): String = "${makerPhoneNumber.value}:${name.value}"
    }

    @Serializable
    data class Name(
        val value: String
    ): Value, Validatable<Name> {
        class IsInvalidException(override val message: String): Exception()

        init {
            throwIfInvalid()
        }

        override fun throwIfInvalid(): Name = this.also {
            if (value.isBlank())
                throw IsInvalidException("Vine Name cannot be blank")
            if (value.length > 28)
                throw IsInvalidException("Vine Name cannot be longer than 28 characters")
        }
    }

    enum class Type {
        RED,
        WHITE
    }
}