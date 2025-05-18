import beans.Api
import interfaces.ILLMJsonExtractor

fun Throwable.toLogString(): String {
    return "message: ${message}\ncause: ${cause}\nstack trace: ${stackTraceToString()}"
}

fun Throwable.println() {
    println("错误:\n${toLogString()}")
}

fun createExtractors(api: Api): List<ILLMJsonExtractor> {
    return api.apiKeys.map { key ->
        object : JsonExtractor(
            key,
            api.baseUrl,
            api.modelName
        ) {
            override fun createPrompt(input: String): String {
                return """
            Extract the edges and nodes from the input text and return a JSON object with two fields: 
            "edges" (list of strings) and "nodes" (list of strings). Ensure the output is valid JSON.
            
            规则: 提取主语或宾语为 node, 谓语或动词为 edge, 不要在 edge 中保留修饰语, 要像语文缩句题目答案一样简单
            
            Input: $input
            
            Example output:
            {
                "edges": ["edge1", "edge2"],
                "nodes": ["node1", "node2"]
            }
            
            为了节省 token, 你可以输出不格式化的 json, 如: {"":{"":""},"":{"":""}}
            
            Return only the JSON object, no additional text.
            """.trimIndent()
            }
        }
    }
}