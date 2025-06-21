package domain.model.value

import eth.likespro.commons.models.Value
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import kotlinx.serialization.Serializable

@Serializable
data class ConversationMetadata(
    val value: String,
): Value {
    companion object {
        fun Any.toConversationMetadata() = ConversationMetadata(this.encodeObject())
    }
    inline fun <reified T> decode() = value.decodeObject<T>()
}