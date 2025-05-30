import beans.Api
import beans.ExtractedData
import beans.GroupedItems
import com.google.gson.GsonBuilder
import interfaces.ICleanDataParser
import interfaces.IDataChucking
import interfaces.IDataWriter
import interfaces.IYamlParser
import kotlinx.coroutines.*
import utils.*

const val INPUT_PATH = "C:\\Users\\NIKI\\Desktop\\clean\\1.json"
const val EXAMPLE_YAML_PATH = "C:\\Users\\NIKI\\Desktop\\clean\\example.yaml"

const val ERROR_DIR = "C:\\Users\\NIKI\\Desktop\\clean\\error"
const val OUTPUT_DIR = "C:\\Users\\NIKI\\Desktop\\clean\\output"

const val CONTEXT = "广东工业大学财务"
const val CHUNK_SIZE = 1024L

const val NEO4J_URL = "neo4j://127.0.0.1:7687/"
const val NEO4J_USERNAME = "neo4j"
const val NEO4J_PASSWORD = "88888888"

val API: Api by lazy {
    Api.Deepseek
//    Api.Zuke("gemini-2.0-flash")
}

/**
 * 主函数: 实现清洗后数据的读取、分组、分块、知识图谱提取和结果导出
 */
fun main(): Unit = runBlocking {
    logW("hello world")

    val gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    // 创建 Main 实例, 负责协调数据处理流程
    val main = Main()
    val dataWriter: IDataWriter by lazy {
        Neo4jDataWriter(NEO4J_URL, NEO4J_USERNAME, NEO4J_PASSWORD)
    }

    try {
        // 读取输入文件并按文件名分组
        val groupedItemsList: List<GroupedItems> = main.readItemsFromFile()

        // 遍历每个分组, 处理数据并生成知识图谱
        groupedItemsList.forEach { groupedItems ->
            // 处理分组数据, 提取节点和关系, 合并结果
            val mergedResult = main.process(groupedItems, CHUNK_SIZE)

            // 将合并结果序列化为 JSON 并写入输出文件
            writeStringToFile(OUTPUT_DIR, groupedItems.filename, gson.toJson(mergedResult))
            dataWriter.writeData(mergedResult)

            // 打印处理完成日志
            logD("${groupedItems.filename} 处理完成")
            logV(mergedResult.toString())
//            logI("已写入文件: ${groupedItems.filename}")
        }

        // 打开输出目录, 方便查看结果
//        logD("打开输出文件夹")
//        openFolder(OUTPUT_DIR)
    } catch (e: Exception) {
        // 捕获主流程异常, 打印错误日志
        logE("主函数运行出错")
        e.logE()
    }

    // 暂停
    pause("已经完成, 按任意键继续")
}

/**
 * Main 类: 协调数据读取、分块、提取和合并的逻辑
 */
class Main {
    // Yaml 解析器
    private val yamlParser: IYamlParser = YamlParser("classes")
    private val yamlClasses: String by lazy {
        val list = yamlParser.readFromPath(EXAMPLE_YAML_PATH)
        prettyGson.toJson(list)
    }

    // 数据解析器, 负责从文件读取和分组清理后的数据
    private val cleanDataParser: ICleanDataParser = CleanDataParser()

    // 数据分块器, 负责将分组数据切分为指定大小的块
    private val chucking: IDataChucking = DataChucking()

    // 数据处理器, 定义上下文和 API, 用于节点和关系提取
    private val dataProcessor = object : DataProcessor() {
        override val context: String = CONTEXT
        override val api: Api = API
    }

    /**
     * 处理单个分组数据: 分块、提取节点/关系、合并结果
     * @return 合并后的知识图谱数据(节点、边、关系)
     */
    suspend fun process(groupedItems: GroupedItems, eachSize: Long = 1024L): ExtractedData {
        // 将分组数据分块
        val chunks = chunk(groupedItems, eachSize)

        // 创建限制并发的 Dispatcher, 基于 dataProcessor 的 keyCounts
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(dataProcessor.keyCounts)

        // 使用协程并发处理每个分块, 提取节点和关系
        return withContext(limitedDispatcher) {
            val results = chunks.map { chunk ->
                async(limitedDispatcher) {
                    processInternal(chunk)
                }
            }.awaitAll()
            // 合并所有分块的提取结果
            merge(results)
        }
    }

    /**
     * 处理单个分块: 提取节点和关系, 组装 ExtractedData
     * @return 提取的知识图谱数据(节点、边、关系)
     */
    private fun processInternal(chunk: String): ExtractedData = try {
        // 提取节点数据
        val nodeData = dataProcessor.parseNodeData(yamlClasses, chunk)
        // 基于节点数据提取关系
        val relationData = dataProcessor.parseRelationNodeData(nodeData, chunk)
        // 组装节点和关系为 ExtractedData
        dataProcessor.buildExtractedData(nodeData, relationData, chunk)
    } catch (t: Throwable) {
        // 捕获处理异常, 记录错误并保存分块到错误目录
        logD("记录为错误", "[${chunk.take(6)}...]")
        t.logE()
        writeStringToFile(ERROR_DIR, "${chunk.hashCode()}.txt", chunk)
        ExtractedData() // 返回空数据, 避免影响后续合并
    }.also {
        // 打印分块处理完成日志
        logD("处理完成", "[${chunk.take(6)}...]")
    }

    /**
     * 合并多个分块的提取结果
     * @return 合并后的知识图谱数据
     */
    private fun merge(list: List<ExtractedData>): ExtractedData = dataProcessor.mergerExtractedData(list)

    /**
     * 读取输入文件并分组
     * @return 分组后的数据列表, 按文件名组织
     */
    fun readItemsFromFile(): List<GroupedItems> {
        // 从指定路径读取数据
        val items = cleanDataParser.readFromPath(INPUT_PATH)
        // 打印读取的数据量
        logI("读取 item 数: ${items.size}")
        // 按文件名分组
        return cleanDataParser.groupByName(items)
    }

    /**
     * 将分组数据分块
     * @return 分块后的字符串列表
     */
    private fun chunk(groupedItems: GroupedItems, size: Long): List<String> {
        // 使用分块器将数据切分为指定大小的块
        val chunks = chucking.formatToChuckedStrings(size, groupedItems)
        // 打印分块数量
        logI("处理 '${groupedItems.filename}' 为 ${chunks.size} 个分块")
        return chunks
    }
}