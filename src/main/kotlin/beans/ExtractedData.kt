package beans

/**
 * 用于写入到 neo4j 的数据类型，包含写入一个关系所需的全部内容
 */
data class ExtractedData(
    val entities: Entities = Entities(null),
    val relations: Relations = Relations(null),
    val datas: List<Data> = emptyList()
) {
    data class Data(
        val entity1: Entity,
        val relation: String,
        val entity2: Entity
    )
}