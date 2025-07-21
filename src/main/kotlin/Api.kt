import Api.Deepseek.DEEPSEEK_KEY
import Api.Deepseek.GEMINI_KEYS
import Api.Deepseek.ZUKE_KEY

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

    data object Deepseek : Api(
        "https://api.deepseek.com/",
        "deepseek-chat",
        DEEPSEEK_KEY
    )

    data object Gemini : Api(
        "https://generativelanguage.googleapis.com/v1beta/openai/",
        "gemini-2.5-flash",
        GEMINI_KEYS
    )

    class Zuke(modelName: String) : Api(
        "https://zuke.chat/v1/",
        modelName,
        ZUKE_KEY
    )
}