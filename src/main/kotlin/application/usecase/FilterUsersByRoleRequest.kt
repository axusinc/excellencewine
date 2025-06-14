package application.usecase

import domain.model.entity.User
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class FilterUsersByRoleRequest(
    val role: User.Role
) {
    suspend fun execute(): List<User> {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        return userRepository.filterByRole(role)
    }
}