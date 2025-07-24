package beans

import utils.Log
import utils.castToLevel

// 最顶层的配置类
data class AppConfig(
    val api: ApiConfig?,
    val paths: PathsConfig?,
    val context: String?,
    val chunkSize: Long?,
    val logLevel: Log.Level?,
    val clearOnStart: Int?,
    val neo4j: Neo4jConfig?,
    val classes: List<ClassDefinition>? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): AppConfig {
            val apiMap = map["api"] as? Map<*, *>
            val pathsMap = map["paths"] as? Map<*, *>
            val context = map["context"] as? String
            val chunkSize = map["chunk-size"] as? Long
            val level = map["log-level"] as? String
            val clearOnStart = map["clear-on-start"] as? Int
            val neo4jMap = map["neo4j"] as? Map<*, *>

            val apiConfig = ApiConfig(
                apiKey = apiMap?.get("api-key") as? String,
                baseUrl = apiMap?.get("base-url") as? String,
                modelName = apiMap?.get("model-name") as? String
            )

            val pathsConfig = PathsConfig(
                inputDir = pathsMap?.get("input-dir") as? String,
                inputPath = pathsMap?.get("input-path") as? String,
                errorDir = pathsMap?.get("error-dir") as? String,
                outputDir = pathsMap?.get("output-dir") as? String
            )

            val neo4jConfig = Neo4jConfig(
                url = neo4jMap?.get("url") as? String,
                username = neo4jMap?.get("username") as? String,
                password = neo4jMap?.get("password") as? String,
            )

            val classes = try {
                @Suppress("UNCHECKED_CAST")
                map["classes"] as? List<ClassDefinition>
            } catch (_: Throwable) {
                null
            }

            return AppConfig(
                apiConfig,
                pathsConfig,
                context,
                chunkSize,
                level?.castToLevel(),
                clearOnStart,
                neo4jConfig,
                classes
            )
        }
    }
}

data class ApiConfig(
    val apiKey: String?,
    val baseUrl: String?,
    val modelName: String?
)

data class PathsConfig(
    val inputDir: String?,
    val inputPath: String?,
    val errorDir: String?,
    val outputDir: String?
)

data class Neo4jConfig(
    val url: String?,
    val username: String?,
    val password: String?
)