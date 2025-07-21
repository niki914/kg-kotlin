package workflow

import beans.ClassDefinition
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import workflow.base.interfaces.IYamlParser
import java.io.File

/**
 * YAML 解析器实现类, 使用 Jackson YAML 解析 YAML 文件
 */
class YamlParser(private val entry: String) : IYamlParser {
    private val yamlMapper = ObjectMapper(YAMLFactory())

    override fun readFromPath(absolutePath: String): List<ClassDefinition> {
        val file = File(absolutePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("文件不存在: $absolutePath")
        }

        return try {
            // 读取 YAML 文件并解析为 Map
            val yamlData = yamlMapper.readValue(file, Map::class.java)
            // 获取 classes 键对应的嵌套 Map
            val classesMap = yamlData[entry] as? Map<*, *>
                ?: throw IllegalStateException("YAML 文件缺少 '$entry' 键: $absolutePath")

            // 转换 classesMap 为 List<ClassDefinition>
            classesMap.map { (className, params) ->
                if (className !is String) {
                    throw IllegalStateException("类名必须是字符串: $className")
                }
                // 确保 params 是 List<String>，并从中创建 Property 列表
                val propertyList = when (params) {
                    is List<*> -> params.map { propertyStr ->
                        if (propertyStr !is String) {
                            throw IllegalStateException("参数格式错误，必须为字符串: $propertyStr")
                        }
                        // 从 "名称(str)" 格式中解析出 name 和 type
                        val parts = propertyStr.split("(", ")")
                        if (parts.size < 2) {
                            throw IllegalStateException("属性格式错误: $propertyStr，应为 '名称(类型)'")
                        }
                        ClassDefinition.Property(name = parts[0], type = parts[1])
                    }

                    else -> throw IllegalStateException("参数列表格式错误: $className")
                }
                // 创建并返回 ClassDefinition 对象
                ClassDefinition(classLabel = className, expectedProperties = propertyList)
            }
        } catch (e: Exception) {
            throw IllegalStateException("解析 YAML 文件失败: $absolutePath, 错误: ${e.message}", e)
        }
    }
}