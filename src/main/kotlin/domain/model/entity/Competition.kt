package domain.model.entity

import eth.likespro.commons.models.Entity
import eth.likespro.commons.models.Validatable
import eth.likespro.commons.models.Value
import eth.likespro.commons.models.value.Timestamp
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Competition(
    override val id: Id = Id(),
    val name: Name,
    val vineType: Vine.Type,
    val startedAt: Timestamp,
    val endedAt: Timestamp?,
    val experts: List<User>,
    val categories: List<Category>,
    val vines: List<Vine>
): Entity<Competition.Id> {
    @Serializable
    data class Id(
        val value: String = UUID.randomUUID().toString()
    ): Value, Validatable<Id> {
        class IsInvalidException(override val message: String): Exception()

        override fun throwIfInvalid(): Id = this.also {
            if(value.length != 36)
                throw IsInvalidException("Competition ID must be exactly 36 characters long")
        }
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
            if (value.isBlank()) {
                throw IsInvalidException("Competition name cannot be blank")
            }
            if (value.length > 40) {
                throw IsInvalidException("Competition name cannot be longer than 40 characters")
            }
        }
    }
}