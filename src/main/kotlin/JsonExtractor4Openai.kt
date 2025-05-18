import beans.ExtractedData
import com.google.gson.Gson
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import interfaces.ILLMJsonExtractor
import interfaces.ILLMJsonExtractor.Companion.cleanJson
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

abstract class JsonExtractor4Openai(
    apiKeys: List<String>,
    baseUrl: String,
    private val model: String
) : ILLMJsonExtractor {
    private val gson = Gson()
    private val clients: List<OpenAIClient> = apiKeys.map { key ->
        OpenAIOkHttpClient.builder()
            .apiKey(key)
            .baseUrl(baseUrl)
            .timeout(Duration.ofSeconds(60))
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

    override fun extract(input: String): ExtractedData {
        // 生成提示词
        val prompt = createPrompt(input)

        // 获取大模型回答, 假设为纯 JSON
        val rawJson = getResponseFromLLM(prompt)

        // 清理 JSON 字符串
        val cleanedJson = cleanJson(rawJson)

        // 解析 JSON 为 ExtractedData
        return gson.fromJson(cleanedJson, ExtractedData::class.java)
            ?: throw IllegalStateException("gson 解析出错")
    }
}