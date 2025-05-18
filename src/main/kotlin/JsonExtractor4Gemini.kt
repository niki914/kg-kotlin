import beans.ExtractedData
import com.google.genai.Client
import com.google.gson.Gson
import interfaces.ILLMJsonExtractor
import interfaces.ILLMJsonExtractor.Companion.cleanJson
import java.util.concurrent.atomic.AtomicInteger

abstract class JsonExtractor4Gemini(
    apiKeys: List<String>,
    private val model: String
) : ILLMJsonExtractor {
    private val gson = Gson()
    private val clients: List<Client> = apiKeys.map {
        Client.builder().vertexAI(false).apiKey(it).build()
    }
    private val currentIndex = AtomicInteger(0)

    override val keyCount: Int
        get() = clients.size

    override fun getResponseFromLLM(prompt: String): String {
        val client = getNextClient()
        val response = client.models.generateContent(model, prompt, null)?.text()
        if (response.isNullOrBlank()) {
            throw IllegalStateException("大模型未返回结果")
        }

        return response
    }

    private fun getNextClient(): Client {
        val index = currentIndex.getAndIncrement() % clients.size
        return clients[index]
    }

    override fun extract(input: String): ExtractedData {
        val prompt = createPrompt(input)
        val rawJson = getResponseFromLLM(prompt)
        val cleanedJson = cleanJson(rawJson)
        return gson.fromJson(cleanedJson, ExtractedData::class.java)
            ?: throw IllegalStateException("gson 解析出错")
    }
}