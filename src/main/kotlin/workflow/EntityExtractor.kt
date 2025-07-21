package workflow

import Api
import beans.ClassDefinition
import beans.Entities
import beans.Entity
import workflow.base.classes.JsonExtractor4Openai
import utils.prettyGson
import utils.tryGetOrNull

/**
 * 节点提取器
 */
class EntityExtractor(
    apiKeys: List<String>,
    baseUrl: String,
    model: String
) : JsonExtractor4Openai<Entities>(apiKeys, baseUrl, model) {
    constructor(api: Api) : this(
        api.apiKeys,
        api.baseUrl,
        api.modelName
    )

    /**
     * context, classes, data
     */
    override fun createPrompt(vararg input: String?): String {
        val context = tryGetOrNull { input[0] } ?: "(context not set, extract all aspects)"
        val classes = tryGetOrNull { input[1] } ?: getDefaultClassDefinition()
        val data = tryGetOrNull { input[2] } ?: "(nothing to process, return a empty json body: '{}')"
        return buildPrompt(context, classes, data)
    }

    override fun extract(vararg input: String?): Entities {
        val json = extractJsonString(*input)
        val entities = json.asJsonClassSafe<Entities>() ?: Entities(null)
        return entities
    }

    private fun buildPrompt(context: String, classes: String, data: String): String {
        /**
         * 你是知识图谱抽取流程中的一个组件。
         * 你的任务是从给定数据中抽取并定义节点。输出必须是有效的JSON对象。
         *
         * 请根据提供的文本和上下文，抽取相关的节点实体。
         *
         * 关键点：所有抽取的实体都**必须**是下方定义的类的实例。
         * 如果某个实体不符合任何给定的类，请将其舍弃。某些类的属性可以为空（null），
         * 但它们必须显式地包含为`null`，而不是被忽略。
         *
         * 类定义（仅抽取属于这些类的实体）：
         * $classes
         *
         * 上下文主题（仅抽取与此主题相关的实体）：
         * $context
         *
         * 输出示例：
         * ${getExample()}
         *
         * 注意事项：
         * 1. 请注意，name、label 和 properties 都是强制性的字段。即使 properties 内部存在一个名称或值相似的属性，你也必须包含顶层的 name 字段。不要合并或省略它。
         * 2. 输出非格式化的JSON以节省token。
         * 3. 不要翻译原始数据中的任何词语，保留其原始语言（处理字符串值时）。
         *
         * 待处理数据：
         * $data
         */
        return """
You are a component in a knowledge graph extraction pipeline.
Your task is to extract and define entities from the given data. The output must be a valid JSON object.

Using the provided text and context, extract relevant node entities.

**Crucially, all extracted entities MUST be instances of the defined classes below.**
If an entity doesn't fit any of the provided classes, discard it. Some class properties can be null, but they must be explicitly included as `null`.

Class Definitions (Only extract entities belonging to these classes):
$classes

Contextual Topic (Only extract entities relevant to this topic):
$context

Example Output:
${getExample()}

Notes:
1. Please note that name, label, and properties are all mandatory fields. If a property with a similar name or value is also present inside properties, you must still include the top-level name field. Do not merge or omit it.
2. Output unformatted JSON to save tokens.
3. DO NOT translate any words in the raw data; keep them in their original language(When processing STRING parameter).

Data to Process:
$data
        """.trimIndent()
    }

    private fun getExample(): String {
        return prettyGson.toJson(
            Entities(
                listOf(
                    Entity(
                        name = "Alice",
                        label = "Person",
                        properties = mapOf(
                            "age" to 20,
                            "job" to "developer"
                        )
                    )
                )
            )
        )
    }

    private fun getDefaultClassDefinition(): String {
        return prettyGson.toJson(
            listOf(ClassDefinition())
        )
    }

    fun extract(classes: List<ClassDefinition>, context: String, data: String): Entities {
        val classesStr = prettyGson.toJson(classes)
        return extract(classesStr, context, data)
    }
}