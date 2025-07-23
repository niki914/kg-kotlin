package workflow

import Api
import beans.ClassDefinition
import beans.Entities
import beans.Entity
import utils.tryGetOrNull
import workflow.base.classes.JsonExtractor4Openai

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
         * 随时变动，请以英文版本为准
         *
         * 你是知识图谱抽取流程中的一个组件。
         * 你的任务是从给定数据中抽取并定义节点实体。输出必须是有效的JSON对象。
         *
         * 请根据提供的文本和上下文，抽取相关的节点实体。
         *
         * 关键约束：所有抽取的实体都**必须**是下方定义的类的实例。
         * 如果某个实体不符合任何给定的类，请将其舍弃。某些类的属性可以为空（null），
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
         * 严格限制：
         * 1. name、label 和 properties 都是强制性字段。即使 properties 内部存在名称或值相似的属性，你也必须包含顶层的 name 字段。不要合并或省略它。
         * 2. 输出非格式化的JSON以节省token。
         * 3. 不要翻译原始数据中的任何词语，保留其原始语言（处理字符串值时）。
         * 4. 确保所有属性都简明扼要，严格控制知识图谱节点附加属性的长度，避免冗长。
         * 5. 确保**不给节点提供含糊的名称**，比如"论文"，应该提供确切的论文标题，否则将严重影响图谱质量。
         *
         * 待处理数据：
         * $data
         */
        return """
# You are a component in a knowledge graph extraction pipeline.

Your task is to extract and define node entities from the given data. The output must be a valid JSON object.

Based on the provided text and context, extract relevant node entities.

# Strict Constraints:
1. name, label, and properties are all mandatory fields. Even if a property with a similar name or value exists inside properties, you must still include the top-level name field. Do not merge or omit it.
2. Output unformatted JSON to save tokens.
3. DO NOT translate any words in the raw data; keep them in their original language (when processing string values).
4. Ensure that all properties are brief and concise, strictly controlling the length of additional properties for knowledge graph nodes to avoid verbosity.
5. Ensure you **don't give nodes vague names** like "论文" - provide the exact thesis title instead, otherwise it will severely impact graph quality.
6. If an entity doesn't fit any of the provided classes, discard it. Some class properties can be null.

## **Critical Constraint: All extracted entities MUST be instances of the defined classes below.**

Class Definitions (Only extract entities belonging to these classes):
$classes

Contextual Topic (Only extract entities relevant to this topic):
$context

## Example Output:
${getExample()}

## Data to Process:
$data
    """.trimIndent()
    }

    private fun getExample(): String {
        return gson.toJson(
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
        return gson.toJson(
            listOf(ClassDefinition())
        )
    }

    fun extract(classes: List<ClassDefinition>, context: String?, data: String): Entities {
        val c = if (context.isNullOrBlank()) {
            null
        } else {
            context
        }
        val classesStr = gson.toJson(classes)
        return extract(c, classesStr, data)
    }
}