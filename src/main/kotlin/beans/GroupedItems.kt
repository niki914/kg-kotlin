package beans

import kotlinx.serialization.Serializable
import utils.convertFilename

// 这些都是处理清洗后的数据用的数据类

@Serializable
data class GroupedItems(
    val filename: String,
    val items: List<Item>
) {
    companion object {
        fun fromSingleString(singleString: String, filename: String): GroupedItems {
            return GroupedItems(
                convertFilename(filename),
                listOf(
                    Item(
                        "SingleString",
                        singleString,
                        Item.Metadata(filename)
                    )
                )
            )
        }
    }
}

@Serializable
data class Item(
    val type: String,
    val text: String,
    val metadata: Metadata
) {
    @Serializable
    data class Metadata(val filename: String)
}