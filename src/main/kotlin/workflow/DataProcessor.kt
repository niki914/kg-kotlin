package workflow

import Api
import beans.*
import utils.logD
import utils.logV
import workflow.base.interfaces.IDataProcessor

abstract class DataProcessor : IDataProcessor {
    abstract val context: String
    abstract val api: Api

    override val keyCounts: Int
        get() = api.apiKeys.size

    private val entityExtractor by lazy {
        EntityExtractor(api)
    }

    private val relationsExtractor by lazy {
        RelationsExtractor(api)
    }

    override fun processChunkToEntities(
        classes: List<ClassDefinition>,
        chunk: String
    ): Entities {
        val entities = entityExtractor.extract(classes, context, chunk)
        logD("节点提取完成", "[${chunk.take(6)}...]")
        return entities
    }

    override fun processChunkToRelations(entities: List<String>, chunk: String): Relations {
        val relations = relationsExtractor.extract(entities, context, chunk)
        logD("关系提取完成", "[${chunk.take(6)}...]")
        return relations
    }

    override fun buildExtractedData(
        entities: Entities,
        relations: Relations,
        chunk: String
    ): ExtractedData {
        val allEntities = entities.entities?.map { it.name } ?: emptyList()

        val filteredRelations = Relations(relations = relations.relations?.filter {
            it.size == 3 && allEntities.contains(it[0]) || allEntities.contains(it[2])
        })

        logD("拼装结果", "[${chunk.take(6)}...]")

        return ExtractedData(entities, filteredRelations, emptyList())
    }

    override fun mergerExtractedData(extractedDatas: List<ExtractedData>): ExtractedData {
        val entities = extractedDatas
            .flatMap { it.entities.entities.orEmpty() }
            .distinct()
            .distinctBy { it.name }
        val relations = extractedDatas
            .flatMap { it.relations.relations.orEmpty() }
            .distinct()

        val datas: List<ExtractedData.Data> = relations.mapNotNull { list ->
            val name1 = list[0]
            val name2 = list[2]

            val entity1 = entities.find { it.name == name1 } ?: Entity(name1)
            val entity2 = entities.find { it.name == name2 } ?: Entity(name2)
            val relation = list[1]

            ExtractedData.Data(entity1, relation, entity2)
        }

        logV("合并结果:\n${datas.joinToString("\n")}")

        return ExtractedData(
            Entities(entities),
            Relations(relations),
            datas
        )
    }
}