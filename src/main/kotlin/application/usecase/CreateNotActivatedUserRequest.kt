package application.usecase

import NotRegisteredUserAlreadyExistsException
import UserAlreadyExistsException
import domain.model.entity.NotActivatedUser
import domain.ports.repositories.NotActivatedUserRepository
import domain.ports.repositories.UserRepository
import eth.likespro.atomarix.Atom.Companion.atomic
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject

@Serializable
data class CreateNotActivatedUserRequest(
    val notActivatedUser: NotActivatedUser
) {
    suspend fun execute() {
        val notActivatedUserRepository: NotActivatedUserRepository by inject(NotActivatedUserRepository::class.java)
        val userRepository: UserRepository by inject(UserRepository::class.java)

        atomic {
            if(notActivatedUserRepository.isExisting(this, notActivatedUser.phoneNumber))
                throw NotRegisteredUserAlreadyExistsException()

            if(userRepository.isExistingByPhoneNumber(this, notActivatedUser.phoneNumber))
                throw UserAlreadyExistsException()

            notActivatedUserRepository.create(this, notActivatedUser)
        }
    }
}