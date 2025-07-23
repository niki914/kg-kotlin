// 偏好 kotlin 代码
package workflow

import beans.ClassDefinition
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File


/**
 * 通用的 YAML 解析器，使用 Jackson Kotlin Module
 */
class YamlParser {

    private val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).apply {
        // 注册 KotlinModule 以支持 Kotlin 数据类
        registerModule(KotlinModule.Builder().build())
        // 注册自定义模块，用于 ClassDefinition 的特殊解析
        val module = SimpleModule()
        // 将自定义的反序列化器关联到 List<ClassDefinition.Property>
        // 注意: 这里直接使用 List::class.java 来注册一个泛型类型会有点问题，Jackson 通常需要知道具体类型。
        // 但由于我们只在 readClasses 方法中处理这种结构，这个注册可能不是必要的，
        // 或者需要更复杂的类型处理。
        // 为了读取整个 Map<String, Any?>，通常不需要为特定的嵌套列表注册全局反序列化器。
        // 如果 `classes` 键下的值类型是 `Map<String, List<String>>` 这种原始形式，
        // 那么默认的 Jackson 行为就能处理。
        // 如果想让它直接返回 List<ClassDefinition>，则需要在 workflow.AppConfig 层面通过注解或单独解析。
        // 为了实现 'readAllAsMap' 的目标，我们暂时不全局注册这个。
        // readClasses 方法会单独处理。
        // module.addDeserializer(List::class.java, ClassDefinitionDeserializer()) // 这行可能会导致问题，因为泛型List太宽泛了
        // registerModule(module)
    }

    /**
     * 从 YAML 文件中读取整个配置，并返回为 Map<String, Any?>。
     * 这提供了最大的灵活性，并避免了对固定数据类的依赖。
     */
    fun readAllAsMap(absolutePath: String): Map<String, Any?> {
        val file = File(absolutePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("文件不存在: $absolutePath")
        }
        return try {
            // 直接读取为 Map<String, Any?>
            // Jackson 会尽力将 YAML 结构映射为 Kotlin 的基本类型（Map, List, String, Number, Boolean）
            val rawData: Map<String, Any?> = yamlMapper.readValue(file, Map::class.java) as Map<String, Any?>

            // 对 'classes' 部分进行特殊处理，使其映射到 List<ClassDefinition>
            val processedData = rawData.toMutableMap()
            if (processedData.containsKey("classes")) {
                val rawClassesMap = processedData["classes"] as? Map<String, List<String>>
                    ?: throw IllegalStateException("YAML 文件 'classes' 键的格式不正确: 期望 Map<String, List<String>>")

                val classDefinitions = rawClassesMap.map { (className, propertiesList) ->
                    val propertyObjects = propertiesList.map { propString ->
                        val parts = propString.split('(', ')', '（', '）').filter { it.isNotBlank() }
                        if (parts.size < 2) {
                            throw IllegalStateException("属性格式错误: '$propString'，应为 '名称(类型)' 或 '名称（类型）'")
                        }
                        ClassDefinition.Property(name = parts[0].trim(), type = parts[1].trim())
                    }
                    ClassDefinition(classLabel = className, expectedProperties = propertyObjects)
                }
                processedData["classes"] = classDefinitions
            }
            processedData
        } catch (e: Exception) {
            throw IllegalStateException("解析 YAML 文件失败: $absolutePath, 错误: ${e.message}", e)
        }
    }
}