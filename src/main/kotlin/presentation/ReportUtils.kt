package presentation

import application.usecase.GetUserRequest
import domain.model.entity.*
import eth.likespro.commons.numeric.NumericUtils.round
import io.github.evanrupert.excelkt.workbook
import kotlinx.coroutines.runBlocking
import presentation.XLSXUtils.boldFontCellStyle
import presentation.XLSXUtils.greenBackgroundStyle
import presentation.XLSXUtils.redFontCellStyle
import java.io.File
import kotlin.math.absoluteValue

object ReportUtils {
    fun generateReport(activeCompetition: Competition, assessments: List<VineAssessment>, prefix: String = ""): Pair<File, Map<Vine.SampleCode, Double>> {
        val finalScores = mutableMapOf<Vine.SampleCode, Double>()
        val vines = activeCompetition.vines.sortedBy { it.name.value }
        val experts = activeCompetition.experts.sortedBy { it.name.value }

        val tmpFile = File("$prefix${activeCompetition.name.value}-report.xlsx")
        workbook {
            sheet("Results") {
                row{
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    experts.forEach { expert -> cell(expert.name.value, boldFontCellStyle()); cell("") }
                }
                row {
                    cell("Sample code", boldFontCellStyle())
                    cell("Producer", boldFontCellStyle())
                    cell("Beverage name", boldFontCellStyle())
                    cell("Vintage/age", boldFontCellStyle())
                    cell("Average score", boldFontCellStyle())
                    cell("Final score", boldFontCellStyle())
                    experts.forEach { _ -> cell("Score", boldFontCellStyle()); cell("Delta", boldFontCellStyle()) }
                }

                vines.forEach { vine ->
                    val scores = mutableMapOf<User.PhoneNumber, Double>()
                    experts.forEach { expert ->
                        val expertAssessments = assessments.filter { it.to == vine.id && it.from == expert.id }
                        if(expertAssessments.size == activeCompetition.categories.size) {
                            val avgScore = expertAssessments.runningReduce { acc, vineAssessment -> acc.copy(mark = acc.mark + computeRealMark(vineAssessment.category, vineAssessment.mark)) }.lastOrNull()
                            if(avgScore != null) scores[expert.id] = avgScore.mark.toDouble()
                        }
                    }
                    val averageScore = scores.values.average()
                    val deltas = mutableMapOf<User.PhoneNumber, Double>()
                    var finalScoreSum = averageScore * scores.values.size
                    var finalScoreParts = scores.values.size
                    experts.forEach { expert ->
                        if (scores.containsKey(expert.id)) {
                            deltas[expert.id] = scores[expert.id]!! - averageScore
                            if(deltas[expert.id]!!.absoluteValue >= 7.0) {
                                finalScoreParts -= 1
                                finalScoreSum -= scores[expert.id]!!
                            }
                        }
                    }
                    val finalScore = finalScoreSum / finalScoreParts
                    finalScores[vine.id] = finalScore

                    row {
                        cell(vine.sampleCode.value)
                        val producer = runBlocking { GetUserRequest(vine.makerPhoneNumber).execute() }!!
                        cell(producer.name.value)
                        cell(vine.name.value)
                        cell("")
                        cell(if(!averageScore.isNaN()) averageScore.round(2).toString() else "-")
                        cell(if(!finalScore.isNaN()) finalScore.round(2).toString() else "-", greenBackgroundStyle())
                        experts.forEach { expert ->
                            if (scores.containsKey(expert.id)) {
                                cell(scores[expert.id]!!.round(2).toString(), if(deltas[expert.id]!!.absoluteValue >= 7.0) redFontCellStyle() else null)
                                cell(deltas[expert.id]!!.round(2).toString(), if(deltas[expert.id]!!.absoluteValue >= 7.0) redFontCellStyle() else null)
                            } else {
                                cell("-")
                                cell("-")
                            }
                        }
                    }
                }
            }
        }.write(tmpFile.path)
        return tmpFile to finalScores
    }

    enum class Medal(val emoji: String) {
        GRAND_PRIX("🏆"),
        GOLD("🥇"),
        SILVER("🥈"),
        BRONZE("🥉"),
        ;
        override fun toString(): String = "$emoji "+when(this) {
            GRAND_PRIX -> "Grand Prix"
            GOLD -> "Gold"
            SILVER -> "Silver"
            BRONZE -> "Bronze"
        }
    }
    fun computeMedal(vineScore: Double): Medal? = when {
        vineScore >= 96 -> Medal.GRAND_PRIX
        vineScore >= 88 -> Medal.GOLD
        vineScore >= 82 -> Medal.SILVER
        vineScore >= 78 -> Medal.BRONZE
        else -> null
    }

    fun computeRealMark(
        category: Category.Name,
        mark: Int,
    ): Int = when(category) {
        (Category.Name("Limpidity")) -> mark // Ok
        (Category.Name("Colour")) -> mark // Ok
        (Category.Name("Aspect other than limpidity")) -> mark * 2 // Ok
        (Category.Name("Effervescence")) -> mark * 2 // Ok
        (Category.Name("Genuineness (Still wines)")) -> mark + 1 // Ok
        (Category.Name("Genuineness (Sparkling wines)")) -> mark + 2 // Ok
        (Category.Name("Nose Typicality")) -> mark + 1 // Ok
        (Category.Name("Nose Positive intensity (Still wines)")) -> when(mark) { // Ok
            1 -> 2
            2 -> 4
            3 -> 6
            4 -> 7
            5 -> 8
            else -> throw IllegalArgumentException("Unknown mark: ${mark}")
        }
        (Category.Name("Nose Positive intensity (Sparkling wines)")) -> mark + 1 // Ok
        (Category.Name("Nose Positive intensity (Spiritous beverages)")) -> (mark * 2) - 1 // Ok
        (Category.Name("Nose Quality (Still wines)")) -> (mark * 2) + 6 // Ok
        (Category.Name("Nose Quality (Sparkling wines)")) -> (mark * 2) + 4 // Ok
        (Category.Name("Nose Quality (Spiritous beverages)")) -> (mark * 2) + 5 // Ok
        (Category.Name("Taste Typicality")) -> mark + 3 // Ok
        (Category.Name("Taste Positive intensity (Still wines)")) -> when(mark) { // Ok
            1 -> 2
            2 -> 4
            3 -> 6
            4 -> 7
            5 -> 8
            else -> throw IllegalArgumentException("Unknown mark: ${mark}")
        }
        (Category.Name("Taste Positive intensity (Sparkling wines)")) -> mark + 2 // Ok
        (Category.Name("Harmonious persistence (Still wines)")) -> when(mark) { // Ok
            1 -> 2
            2 -> 4
            3 -> 6
            4 -> 7
            5 -> 8
            else -> throw IllegalArgumentException("Unknown mark: ${mark}")
        }
        (Category.Name("Harmonious persistence (Sparkling wines)")) -> mark + 2 // Ok
        (Category.Name("Harmonious persistence (Spiritous beverages)")) -> (mark * 2) + 2 // Ok
        (Category.Name("Taste Quality (Still wines)")) -> (mark * 3) + 7 // Ok
        (Category.Name("Taste Quality (Sparkling wines)")) -> (mark * 2) + 4 // Ok
        (Category.Name("Taste Quality (Spiritous beverages)")) -> when(mark) { // Ok
            1 -> 6
            2 -> 10
            3 -> 14
            4 -> 18
            5 -> 20
            else -> throw IllegalArgumentException("Unknown mark: ${mark}")
        }
        (Category.Name("Overall judgement (Still wines)")) -> mark + 6 // Ok
        (Category.Name("Overall judgement (Sparkling wines)")) -> mark + 7 // Ok
        (Category.Name("Overall judgement (Spiritous beverages)")) -> mark
        else -> throw IllegalArgumentException("Unknown category: ${category.value}")
    }
}