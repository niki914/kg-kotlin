package workflow.base.classes

import com.google.gson.JsonParser

abstract class BaseLLMJsonExtractor<T> {

    private fun fetchJsonWithRegex(raw: String): String {
        val markdownRegex = Regex("```.*\\s*([\\s\\S]*?)\\s*```")
        val markdownMatch = markdownRegex.find(raw)

        return if (markdownMatch != null) {
            markdownMatch.groupValues[1].trim()
        } else {
            val jsonRegex = Regex("\\{\\s*([\\s\\S]*?)\\s\\}") // 匹配 JSON 对象的正则
            val jsonMatch = jsonRegex.find(raw)

            jsonMatch?.value?.trim() ?: raw.trim()
        }
    }

    /**
     * 用于处理大模型不听话, 返回了 markdown 代码块包装的 json 时的情况
     */
    protected fun cleanJson(raw: String): Result<String> {
        var cleaned = raw.trim()

        try {
            JsonParser.parseString(cleaned)
            return Result.success(cleaned)
        } catch (_: Exception) {
        }

        cleaned = fetchJsonWithRegex(cleaned)

        try {
            JsonParser.parseString(cleaned)
            return Result.success(cleaned)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("JSON 解析失败: ${e.message}\n$raw"))
        }
    }

    /**
     * 用于让外部参考并控制并发数
     */
    abstract val keyCount: Int

    /**
     * 传入任意多个字符串, 最后返回加工完的提示词, 用于请求
     */
    protected abstract fun createPrompt(vararg input: String?): String

    /**
     * 请求大模型并返回大模型的回答
     */
    protected abstract fun getResponseFromLLM(prompt: String): String

    /**
     * ~~外部应直接调用此方法来提取 json~~
     *
     * 子类应该定义一个参数更安全的版本来暴露给外界
     */
    protected abstract fun extract(vararg input: String?): T
}