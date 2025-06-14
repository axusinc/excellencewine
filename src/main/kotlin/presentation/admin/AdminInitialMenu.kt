package presentation.admin

import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton

fun generateAdminMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton("Керівники експертів"),
            SimpleKeyboardButton("Експерти"),
            SimpleKeyboardButton("Винороби")
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)