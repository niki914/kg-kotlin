package beans

import com.google.gson.annotations.SerializedName

data class Entities(
    @SerializedName("entities") val entities: List<Entity>?
)

/**
 * 定义一个图数据库中的节点。
 *
 * @property name 节点的唯一标识符和显示名称。将作为节点的 'name' 属性用于匹配。即显示在数据库节点上的文本值
 * @property label 节点的主要标签，类似类的概念，例如 "Person", "Database"。
 * @property properties 一个包含节点其他自定义属性的 Map。键是属性名，值可以是任何兼容 Neo4j 的类型。示例： "age" to "20"
 */
data class Entity(
    @SerializedName("name") val name: String?,
    @SerializedName("label") val label: String? = "Entity",
    @SerializedName("properties") val properties: Map<String, Any>? = emptyMap()
)