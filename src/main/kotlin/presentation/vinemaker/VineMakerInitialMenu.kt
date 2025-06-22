package presentation.vinemaker

import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import presentation.CommonStrings

fun generateVineMakerMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton(CommonStrings.COMPETITIONS)
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)
fun generateVineMakerActiveMenu() = ReplyKeyboardMarkup(
    listOf(
        listOf(
            SimpleKeyboardButton(CommonStrings.COMPETITIONS),
            SimpleKeyboardButton(CommonStrings.PREVIEW_RESULTS),
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false
)