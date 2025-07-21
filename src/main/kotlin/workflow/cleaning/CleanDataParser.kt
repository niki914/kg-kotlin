package workflow.cleaning

import beans.GroupedItems
import beans.Item
import kotlinx.serialization.json.Json
import utils.convertFilename
import workflow.base.interfaces.ICleanDataParser
import java.io.File

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
            .filter { it.type == "Formula" || it.type == "NarrativeText" || it.type == "SingleString" }
            .groupBy { it.metadata.filename }
            .map { (filename, groupedItems) ->
                val outputFilename = convertFilename(filename)
                GroupedItems(outputFilename, groupedItems)
            }
    }
}