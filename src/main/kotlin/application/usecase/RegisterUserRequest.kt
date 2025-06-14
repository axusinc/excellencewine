package application.usecase

import CannotGetUserRoleException
import UserAlreadyExistsException
import domain.model.entity.User
import domain.model.value.PhoneNumber
import domain.ports.repositories.NotActivatedUserRepository
import domain.ports.repositories.UserRepository
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class RegisterUserRequest(
    val id: User.Id,
    val phoneNumber: PhoneNumber
) {
    suspend fun execute(): User {
        val notActivatedUserRepository: NotActivatedUserRepository by inject(NotActivatedUserRepository::class.java)
        val userRepository: UserRepository by inject(UserRepository::class.java)

        if(userRepository.isExisting(id))
            throw UserAlreadyExistsException()

        if(userRepository.count() == 0L)
            return userRepository.create(User(id, User.Name("Administrator"), User.Role.ADMIN, phoneNumber))

        val notRegisteredUser = notActivatedUserRepository.findById(phoneNumber)
            ?: throw CannotGetUserRoleException()

        return userRepository.create(User(id, notRegisteredUser.name, notRegisteredUser.role, phoneNumber))
    }
}