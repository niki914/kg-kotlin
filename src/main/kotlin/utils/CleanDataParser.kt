package utils

import beans.GroupedItems
import beans.Item
import interfaces.ICleanDataParser
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths

class CleanDataParser : ICleanDataParser {
    private val json = Json { ignoreUnknownKeys = true }

    override fun readFromPath(absolutePath: String): List<Item> {
        val file = File(absolutePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("文件不存在: $absolutePath")
        }
        val jsonString = file.readText()
        return json.decodeFromString(jsonString)
    }

    override fun groupByName(items: List<Item>): List<GroupedItems> {
        return items
            .filter { it.type == "Formula" || it.type == "NarrativeText" }
            .groupBy { it.metadata.filename }
            .map { (filename, groupedItems) ->
                val outputFilename = convertFilename(filename)
                GroupedItems(outputFilename, groupedItems)
            }
    }

    private fun convertFilename(filename: String): String {
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

    private fun generateSimpleHash(filename: String): String {
        // 获取文件大小
        val file = File(filename)
        val fileSize = if (file.exists()) file.length() else 0L
        // 构造字符串：文件大小|文件名
        val combined = "$fileSize|$filename"
        // 基于拼接字符串生成4位哈希
        val hash = combined.fold(0) { acc, char -> acc + char.code } % 10000
        return hash.toString().padStart(4, '0')
    }
}

