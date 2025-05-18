package utils

import JsonExtractor4Gemini
import JsonExtractor4Openai
import beans.Api
import interfaces.ILLMJsonExtractor
import java.awt.Desktop
import java.io.File
import java.util.*

fun Throwable.toLogString(): String {
    return "message: ${message}\ncause: ${cause}\nstack trace: ${stackTraceToString()}"
}

fun Throwable.logE(tag: String = "") {
    logE("\n" + toLogString(), tag)
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

fun createExtractor(api: Api): ILLMJsonExtractor {
    return when (api) {
        is Api.Gemini -> object : JsonExtractor4Gemini(
            api.apiKeys,
            api.modelName
        ) {
            override fun createPrompt(input: String): String = createPromptInternal(input)
        }

        // 假设其他都支持 openai 格式
        else -> object : JsonExtractor4Openai(
            api.apiKeys,
            api.baseUrl,
            api.modelName
        ) {
            override fun createPrompt(input: String): String = createPromptInternal(input)
        }
    }
}

private fun createPromptInternal(input: String): String {
    return "" +
            "Extract the edges and nodes from the input text and return a JSON object with two fields: \n" +
            "\"edges\" (list of strings) and \"nodes\" (list of strings). Ensure the output is valid JSON." +
            "\n\n" +
            "规则: 提取主语或宾语为 node, 谓语或动词为 edge, 不要在 edge 中保留修饰语, 要像语文缩句题目答案一样简单\n" +
            "输入: [$input]" +
            "\n\n" +
            "Example output:\n" +
            "{\n" +
            "  \"edges\": [\"edge1\", \"edge2\"],\n" +
            "  \"nodes\": [\"node1\", \"node2\"]\n" +
            "}" +
            "\n\n" +
            " 为了节省 token, 你可以输出不格式化的 json, 如: {\"\":{\"\":\"\"},\"\":{\"\":\"\"}}" +
            "\n\n" +
            "Return only the JSON object, no additional text."
}