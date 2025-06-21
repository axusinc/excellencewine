package infrastructure.persistence.tables

import domain.model.entity.Category
import domain.model.entity.User
import domain.model.entity.Vine
import domain.model.entity.VineAssessment
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

object VineAssessmentsTable: Table("vine_assessments") {
    val id = text("id")
    val from = text("from") // User Phone Number
    val to = text("to") // Vine ID
    val category = text("category") // Category Name
    val mark = integer("mark")

    override val primaryKey = PrimaryKey(id)

    fun fromRow(row: ResultRow) = VineAssessment(
        from = User.PhoneNumber(row[from]),
        to = row[to].decodeObject<Vine.Id>(),
        category = Category.Name(row[category]),
        mark = row[mark]
    )

    fun copy(assessment: VineAssessment, to: UpdateBuilder<Int>) {
        to[id] = assessment.id.value // id is a composite of from, to, and category
        to[from] = assessment.from.value
        to[VineAssessmentsTable.to] = assessment.to.encodeObject()
        to[category] = assessment.category.value
        to[mark] = assessment.mark
    }
}