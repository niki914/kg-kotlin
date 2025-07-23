import beans.AppConfig
import beans.GroupedItems
import kotlinx.coroutines.runBlocking
import utils.*
import workflow.YamlParser
import workflow.neo4j.Neo4jWriter

const val EXAMPLE_YAML_PATH = "C:\\Users\\NIKI\\Desktop\\clean\\example.yaml"

val yamlParser = YamlParser()

fun logYamlConfig(path: String) {
    val z = yamlParser.readAllAsMap(path)
    logD("读取到的配置:")
    logD(formatMapToJsonLikeString(z))
}

fun readYamlConfigAsAppConfig(path: String): AppConfig {
    val z = yamlParser.readAllAsMap(EXAMPLE_YAML_PATH)
    val c = AppConfig.fromMap(z)
    logD("读取配置为 AppConfig 实例")
    logD(prettyGson.toJson(c))
    return c
}

/**
 * 主函数: 实现清洗后数据的读取、分组、分块、知识图谱提取和结果导出
 */
fun main(): Unit = runBlocking {
    logW("hello world")
    val appConfig = readYamlConfigAsAppConfig(EXAMPLE_YAML_PATH)
    val neo4jConfig = appConfig.neo4j
    val pathsConfig = appConfig.paths
//    throw Exception("你确定要运行?")

    // 创建 Main 实例, 负责协调数据处理流程
    val workFlow = MainWorkFlow(appConfig)
    /*
        工作流：
        1. 从文件读取清洗后的数据
        2. 处理数据分块
        3. 并发抽取知识图谱
        4. 写入 neo4j 图数据库
     */
    Neo4jWriter(
        neo4jConfig?.url ?: throw ConfigNotSetException("neo4j 参数"),
        neo4jConfig.username ?: throw ConfigNotSetException("neo4j 参数"),
        neo4jConfig.password ?: throw ConfigNotSetException("neo4j 参数")
    ).use { neo4jWriter ->
        neo4jWriter.removeAll()

        try {
            // 读取输入文件并按文件名分组
//            val groupedItemsList: List<GroupedItems> = workFlow.readItemsFromFile() // 单文件模式
            val groupedItemsList: List<GroupedItems> = (pathsConfig?.inputDir
                ?: throw ConfigNotSetException("输入目录")).asFolderAndForEach {
                try {
                    logV("处理: " + it.path)
                    if (it.name.endsWith(".md") || it.name.endsWith(".txt")) {
                        GroupedItems.fromSingleString(it.readText(Charsets.UTF_8), it.name)
                    } else {
                        null
                    }
                } catch (t: Throwable) {
                    logE("遍历文件出错", "", t)
                    null
                }
            }

            // 遍历每个分组, 处理数据并生成知识图谱
            groupedItemsList.forEach { groupedItems ->
                // 处理分组数据, 提取节点和关系, 合并结果
                val mergedResult = workFlow.process(groupedItems, (appConfig.chunkSize ?: 1024L), "未定义的类")

                mergedResult.datas.forEach { data ->
                    neo4jWriter.writeRelation(data.entity1, data.relation, data.entity2)
                }

                // 打印处理完成日志
                logD("${groupedItems.filename} 处理完成")
                logV(mergedResult.toString())
                logI("已写入文件: ${groupedItems.filename}")
            }

        } catch (e: ConfigNotSetException) {
            logE("", "", e)
            logE("请正确配置 yaml 再重新运行")
            return@runBlocking
        } catch (e: Exception) {
            // 捕获主流程异常, 打印错误日志
            logE("主函数运行出错")
            e.logE()
        }
    }

    // 暂停
    pause("图谱抽取已经完成, 按任意键继续")
}