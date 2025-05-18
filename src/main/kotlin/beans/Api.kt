package beans

import config.DEEPSEEK_KEYs
import config.GEMINI_KEYS

enum class Api(
    val baseUrl: String,
    val modelName: String,
    val apiKeys: List<String>
) {
    Deepseek(
        "https://api.deepseek.com/",
        "deepseek-chat",
        DEEPSEEK_KEYs
    ),
    Gemini(
        "https://generativelanguage.googleapis.com/v1beta/openai/",
        "gemini-2.0-flash",
        GEMINI_KEYS
    )
}