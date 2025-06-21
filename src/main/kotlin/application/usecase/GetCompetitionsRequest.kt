package application.usecase

import domain.model.entity.Competition
import domain.ports.repositories.CompetitionRepository
import org.koin.java.KoinJavaComponent.inject

class GetCompetitionsRequest {
    suspend fun execute(): List<Competition> {
        val competitionRepository: CompetitionRepository by inject(CompetitionRepository::class.java)

        return competitionRepository.findAll()
    }
}