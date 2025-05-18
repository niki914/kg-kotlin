package beans

import config.DEEPSEEK_KEYS
import config.GEMINI_KEYS
import config.ZUKE_KEYS

sealed class Api(
    val baseUrl: String,
    val modelName: String,
    val apiKeys: List<String>
) {
    class Deepseek : Api(
        "https://api.deepseek.com/",
        "deepseek-chat",
        DEEPSEEK_KEYS
    )

    class Gemini : Api(
        "https://generativelanguage.googleapis.com/v1beta/",
        "gemini-2.0-flash",
        GEMINI_KEYS
    )

    class Zuke(modelName: String) : Api(
        "https://zuke.chat/v1/",
        modelName,
        ZUKE_KEYS
    )
}