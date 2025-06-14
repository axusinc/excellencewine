package infrastructure.persistence.tables

import domain.model.entity.NotActivatedUser
import domain.model.entity.User
import domain.model.value.PhoneNumber
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

object NotActivatedUsersTable: Table("not_activated_users") {
    val phoneNumber = varchar("phone_number", 200).uniqueIndex()
    val name = varchar("name", 28)
    val role = enumeration("role", User.Role::class)

    override val primaryKey = PrimaryKey(phoneNumber)

    fun fromRow(row: ResultRow) = NotActivatedUser(
        phoneNumber = PhoneNumber(row[phoneNumber]),
        name = User.Name(row[name]),
        role = row[role]
    )

    fun copy(notActivatedUser: NotActivatedUser, to: UpdateBuilder<Int>) {
        to[phoneNumber] = notActivatedUser.phoneNumber.value
        to[name] = notActivatedUser.name.value
        to[role] = notActivatedUser.role
    }
}