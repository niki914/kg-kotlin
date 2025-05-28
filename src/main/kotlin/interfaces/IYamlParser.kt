package interfaces

import beans.YamlClass

/**
 * YAML 解析器接口，定义从指定路径读取并解析 YAML 文件的方法
 */
interface IYamlParser {
    /**
     * 从指定路径读取 YAML 文件并解析为 List<YamlClass>
     * @param absolutePath YAML 文件的绝对路径
     * @return 解析后的 YamlClass 列表
     * @throws IllegalArgumentException 如果文件不存在或不是文件
     */
    fun readFromPath(absolutePath: String): List<YamlClass>
}