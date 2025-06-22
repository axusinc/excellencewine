package presentation.admin

import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import presentation.CommonStrings

fun generateAdminMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton("Керівники експертів"),
            SimpleKeyboardButton("Експерти"),
            SimpleKeyboardButton("Винороби")
        ),
        listOf(
            SimpleKeyboardButton(CommonStrings.START_COMPETITION),
            SimpleKeyboardButton(CommonStrings.COMPETITIONS)
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)

fun generateAdminCompetitionActiveMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton("Керівники експертів"),
            SimpleKeyboardButton("Експерти"),
            SimpleKeyboardButton("Винороби")
        ),
        listOf(
//            SimpleKeyboardButton(CommonStrings.ASSESS),
//            SimpleKeyboardButton(CommonStrings.MY_MARKS),
            SimpleKeyboardButton(CommonStrings.PREVIEW_RESULTS),
            SimpleKeyboardButton(CommonStrings.END_COMPETITION),
        ),
        listOf(
            SimpleKeyboardButton(CommonStrings.COMPETITIONS),
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)