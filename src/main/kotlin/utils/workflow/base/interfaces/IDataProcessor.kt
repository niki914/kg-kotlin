package utils.workflow.base.interfaces

import beans.ClassDefinition
import beans.Entities
import beans.Relations
import beans.Entity


interface IDataProcessor {
    data class ExtractedData2(
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

    val keyCounts: Int

    fun processChunkToEntities(classes: List<ClassDefinition>, chunk: String): Entities
    fun processChunkToRelations(entities: List<String>, chunk: String): Relations
    fun buildExtractedData(
        entities: Entities,
        relations: Relations,
        chunk: String
    ): ExtractedData2

    fun mergerExtractedData(extractedDatas: List<ExtractedData2>): ExtractedData2
}