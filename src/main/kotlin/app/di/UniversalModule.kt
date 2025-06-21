package app.di

import domain.ports.repositories.CompetitionRepository
import domain.ports.repositories.UserRepository
import domain.ports.repositories.VineAssessmentRepository
import infrastructure.persistence.repositories.CompetitionRepositoryImpl
import infrastructure.persistence.repositories.UserRepositoryImpl
import infrastructure.persistence.repositories.VineAssessmentRepositoryImpl
import org.koin.dsl.module

val universalModule = module {
    single { UserRepositoryImpl() as UserRepository }
    single { CompetitionRepositoryImpl() as CompetitionRepository }
    single { VineAssessmentRepositoryImpl() as VineAssessmentRepository }
}