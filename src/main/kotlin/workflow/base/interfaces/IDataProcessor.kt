package workflow.base.interfaces

import beans.*

/**
 * 封装大模型对节点、关系的抽取逻辑
 */
interface IDataProcessor {

    val keyCounts: Int

    fun processChunkToEntities(classes: List<ClassDefinition>, chunk: String): Entities
    fun processChunkToRelations(entities: List<String>, chunk: String): Relations
    fun buildExtractedData(
        entities: Entities,
        relations: Relations,
        chunk: String
    ): ExtractedData

    fun mergerExtractedData(extractedDatas: List<ExtractedData>): ExtractedData
}