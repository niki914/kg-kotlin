import beans.Api
import beans.ExtractedData
import beans.GroupedItems
import com.google.gson.GsonBuilder
import config.ERROR_DIR
import config.INPUT_PATH
import config.LLM_CALL_DELAY
import config.OUTPUT_DIR
import interfaces.ICleanDataParser
import interfaces.IDataChucking
import kotlinx.coroutines.*
import utils.*
import java.io.File


/**
 * 实现清洗后的数据的必要部分获取、数据分块、简单的知识图谱获取和图谱文件导出
 */
fun main(): Unit = runBlocking {
    println("hello world")

    val gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    val parser: ICleanDataParser = CleanDataParser()
    val chucking: IDataChucking = DataChucking()
    val extractors = createExtractors(Api.Deepseek)

    logI("根据密钥数量实例 ${extractors.size} 化个提取器")
    logD("变量初始化完成")

    try {
        // 确保输出目录存在
        File(ERROR_DIR).mkdirs()
        File(OUTPUT_DIR).mkdirs()

        // 读取并分组数据
        val items = parser.readFromPath(INPUT_PATH)
        logI("读取 item 数: ${items.size}")
        val groupedItemsList: List<GroupedItems> = parser.groupByName(items)


        // 遍历每个分组
        groupedItemsList.forEachIndexed { index, groupedItems ->
            if (index != 0) {
                logD("休眠 ${LLM_CALL_DELAY / 1000} 秒, 适应服务器限速")
                delay(LLM_CALL_DELAY)
            }

            // 分块处理
            val chunks = chucking.formatToChuckedStrings(2048L, groupedItems)
            logI("处理 '${groupedItems.filename}' 为 ${chunks.size} 个分块")

            // 提取节点和边
            val results = chunks.mapIndexed { index, chunk ->
                async(Dispatchers.IO) {
                    try {
                        // 轮询分配 extractor
                        val extractor = extractors[index % extractors.size]
                        extractor.extract(chunk)
                    } catch (t: Throwable) {
                        logE("\"${chunk.take(6)}...\" 块记录为 error 文件")
                        t.logE()
                        val outputFile = File(ERROR_DIR, "${chunk.hashCode()}.txt")
                        outputFile.writeText(chunk)
                        ExtractedData(emptyList(), emptyList()) // 返回空结果以继续处理
                    }.also {
                        logD("\"${chunk.take(6)}...\" 块处理完成")
                    }
                }
            }.awaitAll()

            // 合并结果
            val mergedEdges = results.flatMap { it.edges }.distinct()
            val mergedNodes = results.flatMap { it.nodes }.distinct()
            val mergedResult = ExtractedData(mergedEdges, mergedNodes)

            // 写入 JSON 文件
            val outputFile = File(OUTPUT_DIR, groupedItems.filename)
            outputFile.writeText(gson.toJson(mergedResult))

            logD("${groupedItems.filename} 处理完成")
            logI("已写入文件: ${outputFile.absolutePath}")
        }

        logD("打开输出文件夹")
        openFolder(OUTPUT_DIR)
    } catch (e: Exception) {
        logE("主函数运行出错")
        e.logE()
    }

    pause("已经完成, 按任意键继续")
}