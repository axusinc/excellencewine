package presentation

object RetryUtils {
    suspend fun tryWithRetry(retries: Int, delay: Long = 0L, block: suspend () -> Unit) {
        var currentAttempt = 0
        while (currentAttempt < retries) {
            try {
                block()
                return
            } catch (e: Exception) {
                currentAttempt++
                if (currentAttempt >= retries) {
                    throw e
                }
                kotlinx.coroutines.delay(delay)
            }
        }
    }
}