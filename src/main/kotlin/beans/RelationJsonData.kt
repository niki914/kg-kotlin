package beans

import com.google.gson.annotations.SerializedName
import utils.prettyGson

data class RelationJsonData(
    @SerializedName("relations")   val relations: List<List<String>>?
) {
    companion object {
        val EXAMPLE_STRING: String
            get() = prettyGson.toJson(
                RelationJsonData(
                    listOf(
                        listOf("主体1", "关系1", "客体1"),
                        listOf("主体2", "关系2", "客体2")
                    )
                )
            )
    }
}