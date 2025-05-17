package beans

import kotlinx.serialization.Serializable

@Serializable
data class GroupedItems(
    val filename: String,
    val items: List<Item>
)

@Serializable
data class Item(
    val type: String,
    val text: String,
    val metadata: Metadata
) {
    @Serializable
    data class Metadata(val filename: String)
}