package app.di

import domain.ports.repositories.NotActivatedUserRepository
import domain.ports.repositories.UserRepository
import infrastructure.persistence.repositories.NotActivatedUserRepositoryImpl
import infrastructure.persistence.repositories.UserRepositoryImpl
import org.koin.dsl.module

val universalModule = module {
    single { NotActivatedUserRepositoryImpl() as NotActivatedUserRepository }
    single { UserRepositoryImpl() as UserRepository }
}