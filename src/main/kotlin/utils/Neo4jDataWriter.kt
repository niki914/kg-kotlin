package utils

import beans.ExtractedData
import interfaces.IDataWriter
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase

class Neo4jDataWriter(url: String, username: String, password: String) : IDataWriter, AutoCloseable {
    private val driver = GraphDatabase.driver(url, AuthTokens.basic(username, password))

    private fun isConnected(): Boolean {
        return try {
            driver.session().use { session ->
                session.run("RETURN 1").single().get(0).asInt() == 1
            }
        } catch (t: Throwable) {
            logE(t.stackTraceToString(), "isConnected")
            false
        }
    }

    override fun writeData(data: ExtractedData) {
        if (!isConnected()) {
            logE("neo4j 数据库未连接")
            return
        } else {
            logD("neo4j 认证成功")
        }

        TODO("先封装 Cypher 语句的调用工具类")

        val nodes = data.nodes ?: emptyList()
        val relations = data.relations ?: emptyList()

        // 确保 relations 格式正确, 每个关系是三元组
        val allNodeIds =
            (nodes + relations.mapNotNull { it.getOrNull(0) } + relations.mapNotNull { it.getOrNull(2) }).toSet()
                .toList()

        // 修改这里: 将关系数据转换为合适的格式
        val relationsData = relations.mapNotNull { relation ->
            if (relation.size >= 3) {
                mapOf(
                    "startId" to relation[0],
                    "relType" to relation[1],
                    "endId" to relation[2]
                )
            } else null
        }

        try {

            driver.session().use { session ->
                session.writeTransaction { tx ->
                    // 创建节点 - 确保节点ID是字符串
                    val nodeIdsAsStrings = allNodeIds.map { it.toString() }
                    tx.run(
                        "UNWIND \$nodeIds as id MERGE (n:Entity {id: id})",
                        mapOf("nodeIds" to nodeIdsAsStrings)
                    )

                    // 创建关系 - 修改查询以正确处理动态关系类型
                    if (relationsData.isNotEmpty()) {
                        for (relation in relationsData) {
                            val startId = relation["startId"] as String
                            val relType = relation["relType"] as String
                            val endId = relation["endId"] as String

                            // 使用参数化查询, 但动态构建关系类型
                            // 注意: 关系类型需要是有效的标识符, 如果包含特殊字符需要处理
                            val safeRelType = sanitizeRelationType(relType)

                            tx.run(
                                """
                            MATCH (a:Entity {id: ${'$'}startId}), (b:Entity {id: ${'$'}endId})
                            CALL apoc.create.relationship(a, ${'$'}relType, {}, b) YIELD rel
                            RETURN rel
                            """.trimIndent(),
                                mapOf(
                                    "startId" to startId,
                                    "endId" to endId,
                                    "relType" to safeRelType
                                )
                            )
                        }
                    }
                }
            }
            logD("neo4j 写入完成")
        } catch (t: Throwable) {
            logE(t.stackTraceToString(), "writeData")
        }
    }

    // 清理关系类型名称, 确保符合Neo4j标识符规范
    private fun sanitizeRelationType(relType: String): String {
        // 将中文和特殊字符转换为安全的关系类型名
        return relType
            .replace(" ", "_")
            .replace("-", "_")
            .replace("，", "_")
            .replace(",", "_")
            .replace("。", "_")
            .replace(".", "_")
    }

    override fun close() {
        driver.close()
    }
}