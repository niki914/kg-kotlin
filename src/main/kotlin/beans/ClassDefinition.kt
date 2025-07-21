package beans

import com.google.gson.annotations.SerializedName

data class ClassDefinition(
    @SerializedName("class_label") val classLabel: String = "Entity",
    @SerializedName("expected_properties") val expectedProperties: List<Property> = emptyList(),
) {
    data class Property(
        @SerializedName("name") val name: String,
        @SerializedName("type") val type: String,
    )
}