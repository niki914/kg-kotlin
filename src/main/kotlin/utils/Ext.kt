package utils

import JsonExtractor
import beans.Api
import interfaces.ILLMJsonExtractor
import java.awt.Desktop
import java.io.File
import java.util.*

fun Throwable.toLogString(): String {
    return "message: ${message}\ncause: ${cause}\nstack trace: ${stackTraceToString()}"
}

fun Throwable.logE(tag: String = "") {
    logE(toLogString(), tag)
}

fun openFolder(path: String): Boolean = try {
    val folder = File(path)
    if (!folder.exists() || !folder.isDirectory) {
        false
    } else {
        Desktop.getDesktop().open(folder)
        true
    }
} catch (_: Throwable) {
    false
}

fun logV(
    msg: String = "",
    tag: String = ""
) = Log.v(tag, msg)

fun logD(
    msg: String = "",
    tag: String = ""
) = Log.d(tag, msg)

fun logI(
    msg: String = "",
    tag: String = ""
) = Log.i(tag, msg)

fun logW(
    msg: String = "",
    tag: String = ""
) = Log.w(tag, msg)

fun logE(
    msg: String = "",
    tag: String = "",
    throwable: Throwable? = null
) = Log.e(tag, msg, throwable)

private val scanner = Scanner(System.`in`)

fun pause(msg: String = "按任意键继续...") {
    scanner.apply {
        logE(msg, "暂停")
        nextLine()
    }
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