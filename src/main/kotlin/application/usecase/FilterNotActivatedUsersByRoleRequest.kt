package application.usecase

import domain.model.entity.NotActivatedUser
import domain.model.entity.User
import domain.ports.repositories.NotActivatedUserRepository
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class FilterNotActivatedUsersByRoleRequest(
    val role: User.Role
) {
    suspend fun execute(): List<NotActivatedUser> {
        val notActivatedUserRepository: NotActivatedUserRepository by inject(NotActivatedUserRepository::class.java)

        return notActivatedUserRepository.filterByRole(role)
    }
}