import beans.ExtractedData
import com.google.gson.Gson
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import interfaces.ILLMJsonExtractor
import java.time.Duration
import java.util.concurrent.CompletableFuture


abstract class JsonExtractor(
    apiKey: String,
    baseUrl: String,
    private val model: String
) : ILLMJsonExtractor {
    private val client: OpenAIClient = OpenAIOkHttpClient.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .timeout(Duration.ofSeconds(60))
        .build()
    private val gson = Gson()

    override fun extract(input: String): ExtractedData {
        // 生成提示词
        val prompt = createPrompt(input)

        // 构造ChatCompletion请求
        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(ChatModel.of(model))
            .temperature(0.0) // 确保输出严格JSON
            .build()

        // 调用大模型（同步处理异步结果）
        val response: CompletableFuture<com.openai.models.chat.completions.ChatCompletion> =
            client.async().chat().completions().create(params)
        val rawJson = response.get().choices().firstOrNull()?.message()?.content()?.get()
            ?: throw IllegalStateException("大模型未返回结果")

        // 清理JSON字符串
        val cleanedJson = cleanJson(rawJson)

        // 解析JSON为ExtractedData
        return gson.fromJson(cleanedJson, ExtractedData::class.java)
            ?: throw IllegalStateException("gson 解析出错")
    }

    private fun cleanJson(raw: String): String {
        var cleaned = raw.trim()

        // 移除 ```json 或 ``` 前缀和后缀
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").removeSuffix("```").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").removeSuffix("```").trim()
        }

        // 确保是有效的JSON对象
        if (!cleaned.startsWith("{") || !cleaned.endsWith("}")) {
            throw IllegalStateException("模型未返回 json!")
        }

        return cleaned
    }
}