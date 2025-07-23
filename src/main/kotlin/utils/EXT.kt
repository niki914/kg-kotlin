package utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.awt.Desktop
import java.io.File
import java.nio.file.Paths
import java.util.*

fun String.castToLevel(): Log.Level {
    val s = this.lowercase()
    return when (s) {
        "verbose", "v" -> Log.Level.VERBOSE
        "debug", "d" -> Log.Level.DEBUG
        "info", "i" -> Log.Level.INFO
        "warning", "warn", "w" -> Log.Level.WARNING
        "error", "e" -> Log.Level.ERROR
        else -> Log.Level.NONE
    }
}

val prettyGson: Gson by lazy {
    GsonBuilder().setPrettyPrinting().create()
}

fun <T> tryGetOrNull(block: () -> T): T? = try {
    block()
} catch (t: Throwable) {
    t.logE(true)
    null
}

fun Throwable.toSimpleLogString(): String {
    return "message: ${message}\ncause: $cause"
}

fun Throwable.toLogString(): String {
    return "message: ${message}\ncause: ${cause}\nstack trace: ${stackTraceToString()}"
}

fun Throwable.logE(simpleLog: Boolean = false, tag: String = "") {
    val log = if (simpleLog) toSimpleLogString() else toLogString()

    logE("\n" + log, tag)
}

fun formatMapToJsonLikeString(map: Map<String, Any?>, indentLevel: Int = 0): String {
    val sb = StringBuilder()
    val indent = "    ".repeat(indentLevel) // 4 spaces per indent level
    val nextIndent = "    ".repeat(indentLevel + 1)

    sb.append("{\n")

    map.entries.forEachIndexed { index, (key, value) ->
        sb.append(nextIndent)
        sb.append("\"").append(key).append("\": ")

        when (value) {
            is Map<*, *> -> {
                // Recursively format nested maps
                sb.append(formatMapToJsonLikeString(value as Map<String, Any?>, indentLevel + 1))
            }

            is List<*> -> {
                // Format lists
                sb.append("[\n")
                value.forEachIndexed { listIndex, listItem ->
                    sb.append(nextIndent).append("    ") // Extra indent for list items
                    when (listItem) {
                        is String -> sb.append("\"").append(listItem).append("\"")
                        is Number, is Boolean -> sb.append(listItem)
                        null -> sb.append("null")
                        else -> sb.append("\"").append(listItem.toString().replace("\"", "\\\""))
                            .append("\"") // Handle other types as strings
                    }
                    if (listIndex < value.size - 1) {
                        sb.append(",\n")
                    } else {
                        sb.append("\n")
                    }
                }
                sb.append(nextIndent).append("]")
            }

            is String -> {
                // Escape quotes in strings
                sb.append("\"").append(value.replace("\"", "\\\"")).append("\"")
            }

            is Number, is Boolean -> {
                // Numbers and booleans don't need quotes
                sb.append(value)
            }

            null -> {
                // Handle null values
                sb.append("null")
            }

            else -> {
                // Catch-all for other types, convert to string and quote
                sb.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"")
            }
        }

        if (index < map.size - 1) {
            sb.append(",\n")
        } else {
            sb.append("\n")
        }
    }

    sb.append(indent).append("}")
    return sb.toString()
}

fun readFileAsString(filePath: String): String {
    val file = File(filePath)
    if (!file.exists() || !file.isFile) {
        throw Exception()
    }
    return file.readText(Charsets.UTF_8)
}

fun writeStringToFile(parent: String, child: String, content: String) {
    File(parent).mkdirs()
    val outputFile = File(parent, child)
    outputFile.writeText(content)
}

fun <R> String.asFolderAndForEach(block: (File) -> R?): List<R> {
    val folder = File(this)
    return if (folder.isDirectory) {
        // 使用 map 函数来处理和收集结果
        folder.listFiles()
            ?.filter { it.isFile } // 过滤出所有文件
            ?.mapNotNull { file -> block(file) } // 对每个文件执行 block 并将结果映射到新列表
            ?: emptyList() // 如果 listFiles() 为 null，返回空列表
    } else {
        logW("'$this' is not a directory.")
        emptyList()
    }
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

// 把要清洗的文件名改为，比如：a.txt -> a_txt.json
fun convertFilename(filename: String): String {
    val path = Paths.get(filename)
    val name = path.fileName.toString()

    return when {
        name.contains(".") && !name.startsWith(".") -> {
            // 有扩展名的文件，如 test.pdf -> test_pdf.json
            val baseName = name.substringBeforeLast(".")
            val ext = name.substringAfterLast(".")
            if (ext == "txt") {
                // .txt 文件添加四位哈希
                "${baseName}_${generateSimpleHash(name)}.json"
            } else {
                "${baseName}_$ext.json"
            }
        }

        name.startsWith(".") -> {
            // 隐藏文件，如 .txt -> txt_1234.json
            val baseName = name.substring(1)
            "${baseName}_${generateSimpleHash(name)}.json"
        }

        else -> {
            // 无扩展名文件，如 Dockerfile -> Dockerfile.json
            "$name.json"
        }
    }
}

fun generateSimpleHash(filename: String): String {
    // 获取文件大小
    val file = File(filename)
    val fileSize = if (file.exists()) file.length() else 0L
    // 构造字符串：文件大小|文件名
    val combined = "$fileSize|$filename"
    // 基于拼接字符串生成4位哈希
    val hash = combined.fold(0) { acc, char -> acc + char.code } % 10000
    return hash.toString().padStart(4, '0')
}