package interfaces

import beans.ExtractedData
import beans.NodeJsonData
import beans.RelationJsonData

interface IDataProcessor {
    val keyCounts: Int

    fun parseNodeData(classes: String, chunk: String): NodeJsonData
    fun parseRelationNodeData(nodeData: NodeJsonData, chunk: String): RelationJsonData
    fun buildExtractedData(nodeData: NodeJsonData, relationData: RelationJsonData, chunk: String): ExtractedData
    fun mergerExtractedData(processResults: List<ExtractedData>): ExtractedData
}