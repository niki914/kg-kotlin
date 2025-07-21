package utils.workflow.neo4j

import beans.Entity
import org.jetbrains.annotations.ApiStatus.Experimental
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.exceptions.Neo4jException
import utils.*

class Neo4jWriter(url: String, username: String, password: String) : AutoCloseable {

    private val driver: Driver = GraphDatabase.driver(url, AuthTokens.basic(username, password))

    /**
     * 数据库可用性检查
     */
    fun isConnected(): Boolean {
        return try {
            driver.verifyConnectivity()
            logV("数据库连接成功")
            true
        } catch (e: Neo4jException) {
            logE("数据库连接失败: ${e.message}", "", e)
            false
        }
    }

    /**
     * 写入关系 (entity1) --relation-> (entity2)
     */
    fun writeRelation(entity1: Entity, relation: String, entity2: Entity): Boolean {
        if (!isConnected()) return false

        // 使用不同版本需要查询准确的 Cypher 语法
        val query = """
            MERGE (a:`${entity1.label}` {name: ${'$'}entity1Name})
            SET a += ${'$'}entity1Props
            
            MERGE (b:`${entity2.label}` {name: ${'$'}entity2Name})
            SET b += ${'$'}entity2Props
            
            MERGE (a)-[:`${relation}`]->(b)
        """.trimIndent()

        val parameters = mapOf(
            "entity1Name" to entity1.name,
            "entity1Props" to (entity1.properties?.plus(("name" to entity1.name))),
            "entity2Name" to entity2.name,
            "entity2Props" to (entity2.properties?.plus(("name" to entity2.name)))
        )

        logV("query:\n$query")
        logV("parameters:\n$parameters")

        return try {
            driver.session().use { session ->
                session.executeWrite { tx ->
                    // **【修复点】**
                    // 运行查询后，必须调用 .consume() 来消费结果。
                    // 这是完成事务并防止资源泄漏的关键。
                    tx.run(query, parameters).consume()
                }
            }
            logD("写入关系: ($entity1) --$relation-> ($entity2)")
            true
        } catch (e: Neo4jException) {
            logE("写入 Neo4j 时发生错误: ${e.message}", "", e)
            false
        }
    }

    /**
     * 写入关系 (entity1) --relation-> (entity2)
     */
    fun writeRelation(list: List<String>, label: String = "Entity"): Boolean {
        if (list.size != 3) {
            logW("输入列表大小必须为3，当前为: ${list.size}")
            return false
        }
        val entity1 = Entity(name = list[0], label = label)
        val entity2 = Entity(name = list[2], label = label)
        val relation = list[1]
        return writeRelation(entity1, relation, entity2)
    }

    fun foreach(block: (Entity) -> Unit) {
        if (!isConnected()) return

        // Cypher 查询：匹配所有至少有一个 'name' 属性的节点。
        // 这样可以确保我们能正确地构建出 Node 数据类。
        val query = "MATCH (n) WHERE n.name IS NOT NULL RETURN n"

        try {
            driver.session().use { session ->
                session.executeRead { tx ->
                    val result = tx.run(query)
                    // 遍历结果集
                    result.forEach { record ->
                        val neo4jNode = record.get("n").asNode()

                        // 将 Neo4j 内部节点对象转换为我们的 Node 数据类
                        val node = Entity(
                            // 假设 'name' 属性必然存在 (已被 WHERE 子句保证)
                            name = neo4jNode.get("name").asString(),
                            // 获取节点的第一个标签作为主标签，如果没有则为 "Unknown"
                            label = neo4jNode.labels().firstOrNull() ?: "Unknown",
                            // 获取除 'name' 以外的所有属性
                            properties = neo4jNode.asMap() - "name"
                        )
                        // 执行用户传入的 lambda
                        block(node)
                    }
                }
            }
        } catch (e: Neo4jException) {
            logE("遍历 Neo4j 节点时发生错误: ${e.message}", "", e)
        }
    }

    /**
     * 删除所有数据
     */
    fun removeAll() {
        foreach {
            remove(it)
        }
    }

    /**
     *  从数据库中删除一个节点及其所有关联的关系。
     */
    fun remove(node: Entity): Boolean {
        if (!isConnected()) return false

        // Cypher 查询：
        // 1. MATCH 找到具有特定标签和 name 的节点。
        // 2. DETACH DELETE 会在删除节点的同时，先删除所有与该节点相连的关系。
        //    这对于保持图的整洁至关重要，可以避免产生“悬空关系”。
        val query = "MATCH (n:`${node.label}` {name: \$name}) DETACH DELETE n"
        val parameters = mapOf("name" to node.name)

        logV("query:\n$query")
        logV("parameters:\n$parameters")

        return try {
            driver.session().use { session ->
                session.executeWrite { tx ->
                    tx.run(query, parameters).consume()
                }
            }
            logD("删除节点: $node")
            true
        } catch (e: Neo4jException) {
            logE("删除 Neo4j 节点时发生错误: ${e.message}", "", e)
            false
        }
    }

    override fun close() {
        driver.close()
    }

    companion object {
        @Experimental
        fun example() {
            val url = "neo4j://your_ip:7687" // 修改为您的 Neo4j 实例地址
            val username = "neo4j"
            val password = "your_pw"

            Neo4jWriter(url, username, password).use { writer ->

                // 准备一些数据
                logI("--- 准备数据 ---")
                writer.writeRelation(
                    Entity("Alice", "Developer", mapOf("skill" to "Kotlin")),
                    "USES",
                    Entity("Neo4j", "Database")
                )
                writer.writeRelation(Entity("Bob", "Manager"), "MANAGES", Entity("Alice", "Developer"))
                writer.writeRelation(Entity("Charlie", "QA"), "TESTS", Entity("Neo4j", "Database"))
                println()

                // 1. 使用 foreach 遍历当前数据库中的所有节点
                logI("--- 1. 遍历所有节点 ---")
                writer.foreach { node ->
                    println("找到节点 -> Label: ${node.label}, Name: ${node.name}, Properties: ${node.properties}")
                }
                println()

                // 2. 使用 remove 删除一个节点
                logI("--- 2. 删除 'Bob' 节点 ---")
                val nodeToDelete = Entity(name = "Bob", label = "Manager")
                val success = writer.remove(nodeToDelete)
                println("删除操作是否成功: $success\n")

                // 3. 再次遍历，验证 'Bob' 是否已被删除
                logI("--- 3. 再次遍历以验证删除 ---")
                writer.foreach { node ->
                    println("找到节点 -> Label: ${node.label}, Name: ${node.name}, Properties: ${node.properties}")
                }
                println()
            }
        }
    }
}