package beans

data class ExtractedData(
    val nodes: Map<String, YamlClass>? = mapOf(),
    val edges: List<String>? = emptyList(),
    val relations: List<List<String>>? = emptyList(),
)