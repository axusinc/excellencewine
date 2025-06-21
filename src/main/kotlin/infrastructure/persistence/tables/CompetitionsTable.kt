package infrastructure.persistence.tables

import domain.model.entity.Category
import domain.model.entity.Competition
import domain.model.entity.User
import domain.model.entity.Vine
import eth.likespro.commons.models.value.Timestamp
import eth.likespro.commons.reflection.ObjectEncoding.decodeObject
import eth.likespro.commons.reflection.ObjectEncoding.encodeObject
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder

object CompetitionsTable: Table("competitions") {
    val id = varchar("id", 36).uniqueIndex()
    val name = varchar("name", 40)
    val vineType = enumeration("vine_type", Vine.Type::class)
    val startedAt = timestamp("started_at")
    val endedAt = timestamp("ended_at").nullable()
    val experts = text("experts")
    val categories = text("categories")
    val vines = text("vines")

    override val primaryKey = PrimaryKey(id)

    fun fromRow(row: ResultRow): Competition = Competition(
        id = Competition.Id(row[id]),
        name = Competition.Name(row[name]),
        vineType = row[vineType],
        startedAt = Timestamp(row[startedAt]),
        endedAt = row[endedAt]?.let { Timestamp(it) },
        experts = row[experts].decodeObject<List<User>>(),
        categories = row[categories].decodeObject<List<Category>>(),
        vines = row[vines].decodeObject<List<Vine>>()
    )

    fun copy(competition: Competition, to: UpdateBuilder<Int>) {
        to[id] = competition.id.value
        to[name] = competition.name.value
        to[vineType] = competition.vineType
        to[startedAt] = competition.startedAt.toInstant()
        to[endedAt] = competition.endedAt?.toInstant()
        to[experts] = competition.experts.encodeObject()
        to[categories] = competition.categories.encodeObject()
        to[vines] = competition.vines.encodeObject()
    }
}