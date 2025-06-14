package presentation

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import domain.model.entity.User

fun User.Id.toChatId() = ChatId(RawChatId(value))
fun ChatId.toUserId() = User.Id(this.toChatId().chatId.long)