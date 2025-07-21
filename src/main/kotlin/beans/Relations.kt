package beans

import com.google.gson.annotations.SerializedName

data class Relations(
    @SerializedName("relations") val relations: List<List<String>>?
)