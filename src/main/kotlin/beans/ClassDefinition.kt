package beans

import com.google.gson.annotations.SerializedName

/**
 * 给大模型看的 yaml 类定义，需要从 yaml 配置文件读取数据并用来实例化
 */
data class ClassDefinition(
    @SerializedName("class_label") val classLabel: String = "Entity",
    @SerializedName("expected_properties") val expectedProperties: List<Property> = emptyList(),
) {
    data class Property(
        @SerializedName("name") val name: String,
        @SerializedName("type") val type: String,
    )
}