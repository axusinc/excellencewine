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
    val sampleCode: SampleCode,
): Entity<Vine.SampleCode> {
    override val id: SampleCode
        get() = sampleCode

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
            if (value.length > 40)
                throw IsInvalidException("Vine Name cannot be longer than 40 characters")
        }
    }

    @Serializable
    data class SampleCode(
        val value: String
    ): Value, Validatable<SampleCode> {
        class IsInvalidException(override val message: String): Exception()

        init {
            throwIfInvalid()
        }

        override fun throwIfInvalid(): SampleCode = this.also {
            if (value.isBlank())
                throw IsInvalidException("Vine Sample Code cannot be blank")
        }
    }

    enum class Type {
        RED,
        WHITE
    }
}