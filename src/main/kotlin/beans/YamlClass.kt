package beans

import com.google.gson.annotations.SerializedName

/**
 * Data class 表示 YAML 文件中的一个类及其参数列表
 */
data class YamlClass(
    @SerializedName("class") val className: String?,
    @SerializedName("params") val params: List<String>?
)