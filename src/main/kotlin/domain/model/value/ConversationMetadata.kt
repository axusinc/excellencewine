package domain.model.value

import eth.likespro.commons.models.Value
import kotlinx.serialization.Serializable

@Serializable
data class ConversationMetadata(
    val value: String,
): Value