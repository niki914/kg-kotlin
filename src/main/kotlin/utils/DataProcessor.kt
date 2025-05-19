package utils

import beans.Api
import beans.ExtractedData
import beans.NodeJsonData
import beans.RelationJsonData
import interfaces.IDataProcessor
import interfaces.ILLMJsonExtractor

abstract class DataProcessor : IDataProcessor {
    abstract val context: String
    abstract val api: Api

    override val keyCounts: Int
        get() = api.apiKeys.size

    private val nodeExtractor: ILLMJsonExtractor<NodeJsonData> by lazy {
        NodeExtractor(api)
    }

    private val relationsExtractor: ILLMJsonExtractor<RelationJsonData> by lazy {
        RelationsExtractor(api)
    }

    override fun parseNodeData(chunk: String): NodeJsonData {
        val nodeData = nodeExtractor.extract(context, chunk)
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
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val relations = relationData.relations ?: emptyList()

        logD("拼装结果", "[${chunk.take(6)}...]")

        return ExtractedData(edges, nodes, relations)
    }

    override fun mergerExtractedData(processResults: List<ExtractedData>): ExtractedData {
        val mergedEdges = processResults.flatMap { it.edges }.distinct()
        val mergedNodes = processResults.flatMap { it.nodes }.distinct()
        val mergedRelations = processResults.flatMap { it.relations }.distinct()
        mergedRelations.filter {
            val shouldFilter = (it.size == 3)
                    && it[1] in mergedEdges
                    && it[0] in mergedNodes
                    && it[2] in mergedNodes
            if (shouldFilter) {
                logW("过滤: $it")
            }

            shouldFilter
        }

        return ExtractedData(mergedEdges, mergedNodes, mergedRelations)
    }
}