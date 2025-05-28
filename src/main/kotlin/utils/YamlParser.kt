package utils

import beans.YamlClass
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import interfaces.IYamlParser
import java.io.File

/**
 * YAML 解析器实现类, 使用 Jackson YAML 解析 YAML 文件
 */
class YamlParser(private val entry: String) : IYamlParser {
    private val yamlMapper = ObjectMapper(YAMLFactory()).apply {
        // 忽略未知字段, 增加鲁棒性
        // 注意: Jackson YAML 不需要显式设置 ignoreUnknownKeys, 默认为宽松解析
    }

    override fun readFromPath(absolutePath: String): List<YamlClass> {
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

            // 转换 classesMap 为 List<YamlClass>
            classesMap.map { (className, params) ->
                if (className !is String) {
                    throw IllegalStateException("类名必须是字符串: $className")
                }
                // 确保 params 是 List<String>, 处理可能的类型不匹配
                val paramList = when (params) {
                    is List<*> -> params.filterIsInstance<String>()
                    else -> throw IllegalStateException("参数列表格式错误: $className")
                }
                YamlClass(className, paramList)
            }
        } catch (e: Exception) {
            throw IllegalStateException("解析 YAML 文件失败: $absolutePath, 错误: ${e.message}", e)
        }
    }
}