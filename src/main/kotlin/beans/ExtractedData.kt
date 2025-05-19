package beans

data class ExtractedData(
    val edges: List<String> = emptyList(),
    val nodes: List<String> = emptyList(),
    val relations: List<List<String>> = emptyList(),
)