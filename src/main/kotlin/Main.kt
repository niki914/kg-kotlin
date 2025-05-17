import beans.ExtractedData
import beans.GroupedItems
import com.google.gson.Gson
import interfaces.ICleanDataParser
import interfaces.IDataChucking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 实现清洗后的数据的必要部分获取、数据分块、简单的知识图谱获取和图谱文件导出
 */
fun main() = runBlocking {
    val inputPath = "C:\\Users\\NIKI\\Desktop\\clean\\1.json"
    val errorDir = "C:\\Users\\NIKI\\Desktop\\clean\\error"
    val outputDir = "C:\\Users\\NIKI\\Desktop\\clean\\output"

    val parser: ICleanDataParser = CleanDataParser()
    val chucking: IDataChucking = DataChucking()
    val extractors = DEEPSEEK_KEYs.map { key ->
        object : JsonExtractor(
            key,
            "https://api.deepseek.com/",
            "deepseek-chat"
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

    println("实例 ${extractors.size} 化个 extractor")

    val gson = Gson()

    try {
        // 确保输出目录存在
        File(errorDir).mkdirs()
        File(outputDir).mkdirs()

        // 读取并分组数据
        val items = parser.readFromPath(inputPath)
        println("读取 item 数: ${items.size}")
        val groupedItemsList: List<GroupedItems> = parser.groupByName(items)

        // 遍历每个分组
        groupedItemsList.forEach { groupedItems ->
            // 分块处理
            val chunks = chucking.formatToChuckedStrings(2048L, groupedItems)
            println("处理 '${groupedItems.filename}' 为 ${chunks.size} 个分块")

            // 提取节点和边
            val results = chunks.mapIndexed { index, chunk ->
                async(Dispatchers.IO) {
                    try {
                        // 轮询分配 extractor
                        val extractor = extractors[index % extractors.size]
                        extractor.extract(chunk)
                    } catch (t: Throwable) {
                        t.println()
                        val outputFile = File(errorDir, "${chunk.hashCode()}.txt")
                        println("\"${chunk.take(6)}...\" 块记录为 error 文件")
                        outputFile.writeText(chunk)
                        ExtractedData(emptyList(), emptyList()) // 返回空结果以继续处理
                    }.also {
                        println("\"${chunk.take(6)}...\" 块处理完成")
                    }
                }
            }.awaitAll()

            // 合并结果
            val mergedEdges = results.flatMap { it.edges }.distinct()
            val mergedNodes = results.flatMap { it.nodes }.distinct()
            val mergedResult = ExtractedData(mergedEdges, mergedNodes)

            // 写入 JSON 文件
            val outputFile = File(outputDir, groupedItems.filename)
            outputFile.writeText(gson.toJson(mergedResult))
            println("已写入文件: ${outputFile.absolutePath}")
        }
    } catch (e: Exception) {
        e.println()
    }
}