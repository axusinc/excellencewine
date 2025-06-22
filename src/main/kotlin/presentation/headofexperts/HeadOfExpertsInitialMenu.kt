package presentation.headofexperts

import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import presentation.CommonStrings

fun generateHeadOfExpertsMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton(CommonStrings.START_COMPETITION),
            SimpleKeyboardButton(CommonStrings.COMPETITIONS)
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)
fun generateHeadOfExpertsActiveMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton(CommonStrings.PREVIEW_RESULTS),
            SimpleKeyboardButton(CommonStrings.END_COMPETITION),
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)