package domain.model.entity

import domain.model.entity.User.Name.IsInvalidException
import eth.likespro.commons.models.Entity
import eth.likespro.commons.models.Validatable
import eth.likespro.commons.models.Value
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val name: Name
): Entity<Category.Name> {
    override val id: Name
        get() = name

    @Serializable
    data class Name(
        val value: String
    ): Value, Validatable<Name> {
        class IsInvalidException(message: String) : Exception(message)

        init {
            throwIfInvalid()
        }

        override fun throwIfInvalid(): Name = this.also {
            if (value.isBlank())
                throw domain.model.entity.User.Name.IsInvalidException("Name cannot be blank")
            if (value.length > 28)
                throw domain.model.entity.User.Name.IsInvalidException("Name cannot be longer than 28 characters")
        }
    }
}