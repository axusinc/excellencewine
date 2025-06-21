package presentation

import domain.model.entity.User

object CommonStrings {
    const val BACK_BUTTON_TEXT = "⬅️ Назад"
    const val NEXT_BUTTON_TEXT = "➡️ Далі"
    const val ERROR_UNKNOWN = "Виникла невідома помилка. Спробуйте ще раз через 15 секунд або зверніться до адміністратора."

    const val COMPETITIONS = "Конкурси"

    const val START_COMPETITION = "Почати конкурс"
    const val END_COMPETITION = "Завершити конкурс"

    const val ASSESS = "Поставити оцінку"
    const val MY_MARKS = "Мої оцінки"
    const val PREVIEW_RESULTS = "Попередні результати"
    fun getRoleReadableName(role: User.Role) = when(role) {
        User.Role.ADMIN -> "Адміністратор"
        User.Role.HEAD_OF_EXPERTS -> "Керівник експертів"
        User.Role.EXPERT -> "Експерт"
        User.Role.VINE_MAKER -> "Винороб"
    }
    fun getRoleFromReadableName(role: String): User.Role? = when(role.lowercase()) {
        "адміністратор" -> User.Role.ADMIN
        "керівник експертів" -> User.Role.HEAD_OF_EXPERTS
        "експерт" -> User.Role.EXPERT
        "винороб" -> User.Role.VINE_MAKER
        "адміністратори" -> User.Role.ADMIN
        "керівники експертів" -> User.Role.HEAD_OF_EXPERTS
        "експерти" -> User.Role.EXPERT
        "винороби" -> User.Role.VINE_MAKER
        else -> null
    }
}