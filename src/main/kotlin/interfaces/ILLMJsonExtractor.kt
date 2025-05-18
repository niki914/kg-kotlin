package interfaces

import beans.ExtractedData
import iLLMJsonExtractorExample

/**
 * @sample iLLMJsonExtractorExample
 */
interface ILLMJsonExtractor {
    companion object {
        fun cleanJson(raw: String): String {
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

    val keyCount: Int

    fun createPrompt(input: String): String
    fun getResponseFromLLM(prompt: String): String
    fun extract(input: String): ExtractedData
}