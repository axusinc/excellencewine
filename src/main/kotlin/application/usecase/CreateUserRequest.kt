package application.usecase

import UserAlreadyExistsException
import domain.model.entity.User
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class CreateUserRequest(
    val user: User
) {
    suspend fun execute(): User {
        val userRepository: UserRepository by inject(UserRepository::class.java)

        if(userRepository.isExisting(user.phoneNumber))
            throw UserAlreadyExistsException()

        return userRepository.create(user)
    }
}