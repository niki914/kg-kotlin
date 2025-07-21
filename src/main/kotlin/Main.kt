import beans.GroupedItems
import kotlinx.coroutines.runBlocking
import utils.*
import workflow.neo4j.Neo4jWriter

const val INPUT_PATH = "C:\\Users\\NIKI\\Desktop\\clean\\1.json"
const val EXAMPLE_YAML_PATH = "C:\\Users\\NIKI\\Desktop\\clean\\example.yaml"

const val ERROR_DIR = "C:\\Users\\NIKI\\Desktop\\clean\\error"
const val OUTPUT_DIR = "C:\\Users\\NIKI\\Desktop\\clean\\output"

const val CONTEXT = "广东工业大学财务"
const val CHUNK_SIZE = 1024L

const val NEO4J_URL = "neo4j://10.24.2.101:7687/"
const val NEO4J_USERNAME = "neo4j"
const val NEO4J_PASSWORD = "topview@624"

val API: Api by lazy {
//    Api.Deepseek
//    Api.Gemini
    Api.Zuke("gemini-2.0-flash")
}

/**
 * 主函数: 实现清洗后数据的读取、分组、分块、知识图谱提取和结果导出
 */
fun main(): Unit = runBlocking {
    logW("hello world")

//    val gson by lazy {
//        GsonBuilder().setPrettyPrinting().create()
//    }

    // 创建 Main 实例, 负责协调数据处理流程
    val workFlow = MainWorkFlow()
    /*
        工作流：
        1. 从文件读取清洗后的数据
        2. 处理数据分块
        3. 并发抽取知识图谱
        4. 写入 neo4j 图数据库
     */
    Neo4jWriter(NEO4J_URL, NEO4J_USERNAME, NEO4J_PASSWORD).use { neo4jWriter ->

//    neo4jWriter.removeAll()

        try {
            // 读取输入文件并按文件名分组
            val groupedItemsList: List<GroupedItems> = workFlow.readItemsFromFile()

            // 遍历每个分组, 处理数据并生成知识图谱
            groupedItemsList.forEach { groupedItems ->
                // 处理分组数据, 提取节点和关系, 合并结果
                val mergedResult = workFlow.process(groupedItems, CHUNK_SIZE)

                // 将合并结果序列化为 JSON 并写入输出文件
//                writeStringToFile(OUTPUT_DIR, groupedItems.filename, gson.toJson(mergedResult))
                mergedResult.datas.forEach { data ->
                    neo4jWriter.writeRelation(data.entity1, data.relation, data.entity2)
                }

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
    }

    // 暂停
    pause("图谱抽取已经完成, 按任意键继续")
}