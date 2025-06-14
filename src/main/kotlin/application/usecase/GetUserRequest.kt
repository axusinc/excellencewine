package application.usecase

import domain.model.entity.User
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

@Serializable
data class GetUserRequest(
    val id: User.Id
) {
    suspend fun execute(): User? {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        return userRepository.findById(id)
    }
}