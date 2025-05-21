package beans

import config.DEEPSEEK_KEY
import config.GEMINI_KEYS
import config.ZUKE_KEY

sealed class Api(
    val baseUrl: String,
    val modelName: String,
    val apiKeys: List<String>
) {
    constructor(
        baseUrl: String,
        modelName: String,
        apiKey: String
    ) : this(baseUrl, modelName, listOf(apiKey))

    class Deepseek : Api(
        "https://api.deepseek.com/",
        "deepseek-chat",
        DEEPSEEK_KEY
    )

    class Gemini : Api(
        "https://generativelanguage.googleapis.com/v1beta/",
        "gemini-2.0-flash",
        GEMINI_KEYS
    )

    class Zuke(modelName: String) : Api(
        "https://zuke.chat/v1/",
        modelName,
        ZUKE_KEY
    )
}