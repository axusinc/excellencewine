package domain.model.entity

import eth.likespro.commons.models.Entity
import eth.likespro.commons.models.Validatable
import eth.likespro.commons.models.Value
import kotlinx.serialization.Serializable

@Serializable
data class Vine(
    val makerPhoneNumber: User.PhoneNumber?,
    val name: Name?,
    val type: Type,
    val sampleCode: SampleCode,
    val realType: RealType
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

    enum class RealType {
        STILL,
        SPARKLING,
        SPIRITOUS,
        ;
        companion object {
            fun fromString(value: String): RealType? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }

        fun getCategories(): List<Category> = when(this) {
            Vine.RealType.STILL -> listOf(
                Category(Category.Name("Limpidity")),
                Category(Category.Name("Aspect other than limpidity")),
                Category(Category.Name("Genuineness (Still wines)")),
                Category(Category.Name("Nose Positive intensity (Still wines)")),
                Category(Category.Name("Nose Quality (Still wines)")),
                Category(Category.Name("Taste Positive intensity (Still wines)")),
                Category(Category.Name("Harmonious persistence (Still wines)")),
                Category(Category.Name("Taste Quality (Still wines)")),
                Category(Category.Name("Overall judgement (Still wines)")),
            )

            Vine.RealType.SPARKLING -> listOf(
                Category(Category.Name("Limpidity")),
                Category(Category.Name("Aspect other than limpidity")),
                Category(Category.Name("Effervescence")),
                Category(Category.Name("Genuineness (Sparkling wines)")),
                Category(Category.Name("Nose Positive intensity (Sparkling wines)")),
                Category(Category.Name("Nose Quality (Sparkling wines)")),
                Category(Category.Name("Taste Positive intensity (Sparkling wines)")),
                Category(Category.Name("Harmonious persistence (Sparkling wines)")),
                Category(Category.Name("Taste Quality (Sparkling wines)")),
                Category(Category.Name("Overall judgement (Sparkling wines)")),
            )

            Vine.RealType.SPIRITOUS -> listOf(
                Category(Category.Name("Limpidity")),
                Category(Category.Name("Colour")),
                Category(Category.Name("Nose Typicality")),
                Category(Category.Name("Nose Positive intensity (Spiritous beverages)")),
                Category(Category.Name("Nose Quality (Spiritous beverages)")),
                Category(Category.Name("Taste Typicality")),
                Category(Category.Name("Harmonious persistence (Spiritous beverages)")),
                Category(Category.Name("Taste Quality (Spiritous beverages)")),
                Category(Category.Name("Overall judgement (Spiritous beverages)")),
            )
        }
    }
}