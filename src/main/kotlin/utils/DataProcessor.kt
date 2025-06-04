package utils

import beans.*
import interfaces.IDataProcessor

abstract class DataProcessor : IDataProcessor {
    abstract val context: String
    abstract val api: Api

    override val keyCounts: Int
        get() = api.apiKeys.size

    private val nodeExtractor: BaseLLMJsonExtractor<NodeJsonData> by lazy {
        NodeExtractor(api)
    }

    private val relationsExtractor: BaseLLMJsonExtractor<RelationJsonData> by lazy {
        RelationsExtractor(api)
    }

    override fun parseNodeData(classes: String, chunk: String): NodeJsonData {
        val nodeData = nodeExtractor.extract(context, classes, chunk)
        logD("节点提取完成", "[${chunk.take(6)}...]")
        return nodeData
    }

    override fun parseRelationNodeData(nodeData: NodeJsonData, chunk: String): RelationJsonData {
        val nodesString = nodeData.entities
            ?.mapNotNull { it.name }
            ?.filter { it.isNotBlank() }
            ?.joinToString(", ") // 连接为 "xxx, xxx, ..."

        val relationData = relationsExtractor.extract(nodesString, context, chunk)
        logD("关系提取完成", "[${chunk.take(6)}...]")
        return relationData
    }

    override fun buildExtractedData(
        nodeData: NodeJsonData,
        relationData: RelationJsonData,
        chunk: String
    ): ExtractedData {
        val nodes = nodeData.entities
            ?.mapNotNull { it.name }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val edges = relationData.relations
            ?.map { it[1] }
            ?.mapNotNull { edgeName ->
                val yamlClass = nodeData.entities?.find { it.name == edgeName }?.yamlClass
                if (!yamlClass?.className.isNullOrBlank()) {
                    edgeName to yamlClass!!
                } else {
                    null
                }
            }
            ?.toMap()
            ?: emptyMap()

        val relations = relationData.relations ?: emptyList()

        logD("拼装结果", "[${chunk.take(6)}...]")

        return ExtractedData(edges, nodes, relations)
    }

    override fun mergerExtractedData(processResults: List<ExtractedData>): ExtractedData {
        val mergedNodes = mutableMapOf<String, YamlClass>()
        processResults.forEach { result ->
            result.nodes?.let { edges ->
                mergedNodes.putAll(edges)
            }
        }
        val mergedEdges = processResults.mapNotNull { it.edges }.flatten().distinct()
        val mergedRelations = processResults.mapNotNull { it.relations }.flatten().distinct()
        mergedRelations.filter {
            val shouldFilter = (it.size == 3)
                    && it[1] in mergedEdges
                    && it[0] in mergedNodes.keys
                    && it[2] in mergedNodes.keys
            if (shouldFilter) {
                logW("过滤: $it")
            }

            shouldFilter
        }

        return ExtractedData(mergedNodes, mergedEdges, mergedRelations)
    }
}