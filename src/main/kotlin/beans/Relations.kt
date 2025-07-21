package beans

import com.google.gson.annotations.SerializedName

/**
 * 大模型应该返回的三元关系组，用 gson 序列化
 */
data class Relations(
    @SerializedName("relations") val relations: List<List<String>>?
)