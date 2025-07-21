package workflow.base.classes

abstract class BaseLLMJsonExtractor<T> {

    /**
     * 用于处理大模型不听话, 返回了 markdown 代码块包装的 json 时的情况
     */
    protected fun cleanJson(raw: String): String {
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