import beans.ClassDefinition
import beans.ExtractedData
import beans.GroupedItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import utils.*
import workflow.DataProcessor
import workflow.YamlParser
import workflow.base.interfaces.ICleanDataParser
import workflow.base.interfaces.IDataChucking
import workflow.base.interfaces.IYamlParser
import workflow.cleaning.CleanDataParser
import workflow.cleaning.DataChucking

/**
 * MainWorkFlow 类: 协调数据读取、分块、提取和合并的逻辑
 */
class MainWorkFlow {
    // Yaml 解析器
    private val yamlParser: IYamlParser = YamlParser("classes")
    private val classDefinitions: List<ClassDefinition> by lazy {
        yamlParser.readFromPath(EXAMPLE_YAML_PATH)
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
    suspend fun process(
        groupedItems: GroupedItems,
        eachSize: Long = 1024L,
        unknownEntityTag: String = "Entity"
    ): ExtractedData {
        // 将分组数据分块
        val chunks = chunk(groupedItems, eachSize)

        // 创建限制并发的 Dispatcher, 基于 dataProcessor 的 keyCounts
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(dataProcessor.keyCounts)

        // 使用协程并发处理每个分块, 提取节点和关系
        return withContext(limitedDispatcher) {
            val results = chunks.map { chunk ->
                async(limitedDispatcher) {
                    processInternal(chunk).also {
                        logV("processInternal:\n$it")
                    }
                }
            }.awaitAll()
            // 合并所有分块的提取结果
            merge(results, unknownEntityTag)
        }
    }

    /**
     * 处理单个分块: 提取节点和关系, 组装 ExtractedData
     * @return 提取的知识图谱数据(节点、边、关系)
     */
    private fun processInternal(chunk: String): ExtractedData = try {
        // 提取节点数据
        val entites = dataProcessor.processChunkToEntities(classDefinitions, chunk)

        // 基于节点数据提取关系
        val relations = dataProcessor.processChunkToRelations(
            entites.entities
                ?.mapNotNull { it.name }
                ?.distinct() ?: emptyList(), chunk
        )

        // 组装节点和关系为 ExtractedData
        dataProcessor.buildExtractedData(entites, relations, chunk)
    } catch (t: Throwable) {
        // 捕获处理异常, 记录错误并保存分块到错误目录
        logD("记录为错误", "[${chunk.take(6)}...]")
        t.logE()
        writeStringToFile(ERROR_DIR, "${chunk.hashCode()}.txt", chunk)
        ExtractedData()
    }.also {
        // 打印分块处理完成日志
        logD("处理完成", "[${chunk.take(6)}...]")
    }

    /**
     * 合并多个分块的提取结果
     * @return 合并后的知识图谱数据
     */
    private fun merge(list: List<ExtractedData>, unknownEntityTag: String = "Entity"): ExtractedData =
        dataProcessor.mergerExtractedData(list, unknownEntityTag)

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