package utils.workflow.base.classes

import Api
import com.google.gson.Gson
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import utils.logV
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

abstract class JsonExtractor4Openai<T>(
    apiKeys: List<String>,
    baseUrl: String,
    private val model: String
) : BaseLLMJsonExtractor<T>() {
    constructor(api: Api) : this(
        api.apiKeys,
        api.baseUrl,
        api.modelName
    )

    companion object {
        const val CLIENT_TIMEOUT_SEC: Long = 30
    }

    protected val gson = Gson()

    private val clients: List<OpenAIClient> = apiKeys.map { key ->
        OpenAIOkHttpClient.builder()
            .apiKey(key)
            .baseUrl(baseUrl)
            .timeout(Duration.ofSeconds(CLIENT_TIMEOUT_SEC))
            .build()
    }

    private val currentIndex = AtomicInteger(0)

    override val keyCount: Int
        get() = clients.size

    override fun getResponseFromLLM(prompt: String): String {
        // 获取下一个客户端
        val client = getNextClient()

        // 构造 ChatCompletion 请求
        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(ChatModel.of(model))
            .temperature(0.3)
            .build()

        val response: CompletableFuture<com.openai.models.chat.completions.ChatCompletion> =
            client.async().chat().completions().create(params)
        return response.get().choices().firstOrNull()?.message()?.content()?.get()
            ?: throw IllegalStateException("大模型未返回结果")
    }

    private fun getNextClient(): OpenAIClient {
        val index = currentIndex.getAndIncrement() % clients.size
        return clients[index]
    }

    protected fun extractJsonString(vararg input: String?): String {
        // 生成提示词
        val prompt = createPrompt(*input)
        logV("提示词:\n$prompt")

        // 获取大模型回答, 假设为纯 JSON
        val rawJson = getResponseFromLLM(prompt)

        logV("大模型回答:\n$rawJson")

        // 清理 JSON 字符串
        val cleanedJson = cleanJson(rawJson)

        return cleanedJson
    }

    protected inline fun <reified D> String.asJsonClass(): D =
        gson.fromJson(this, D::class.java) ?: throw IllegalStateException("gson 解析出错")

    protected inline fun <reified D> String.asJsonClassSafe(): D? = try {
        asJsonClass<D>()
    } catch (_: Throwable) {
        null
    }
}