package presentation.expert

import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import presentation.CommonStrings

fun generateExpertMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton(CommonStrings.COMPETITIONS)
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)
fun generateExpertActiveMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton(CommonStrings.ASSESS),
            SimpleKeyboardButton(CommonStrings.MY_MARKS),
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)