package application.usecase

import domain.model.entity.User
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class UpdateUserRequest(
    val user: User
) {
    suspend fun execute(): User? {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        return userRepository.update(user)
    }
}