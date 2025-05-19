package beans

data class NodeJsonData(
    val entities: List<Node>?
)

data class Node(
    val name: String?,
    val description: String?,
)