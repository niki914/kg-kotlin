package workflow

import Api
import beans.Relations
import workflow.base.classes.JsonExtractor4Openai
import utils.prettyGson
import utils.tryGetOrNull

/**
 * 关系提取器
 */
class RelationsExtractor(
    apiKeys: List<String>,
    baseUrl: String,
    model: String
) : JsonExtractor4Openai<Relations>(apiKeys, baseUrl, model) {
    constructor(api: Api) : this(
        api.apiKeys,
        api.baseUrl,
        api.modelName
    )

    /**
     * given nodes, context, data
     */
    override fun createPrompt(vararg input: String?): String {
        val givenEntities = tryGetOrNull { input[0] } ?: "(entities not defined, return a empty json body: '{}')"
        val context = tryGetOrNull { input[1] } ?: "(context not set, extract all aspects)"
        val data = tryGetOrNull { input[2] } ?: "(nothing to process, return a empty json body: '{}')"
        return buildPrompt(givenEntities, context, data)
    }

    override fun extract(vararg input: String?): Relations {
        val json = extractJsonString(*input)
        val relations = json.asJsonClassSafe<Relations>() ?: Relations(null)
        return relations
    }

    private fun buildPrompt(givenEntities: String, context: String, data: String): String {
        /**
         * 你是知识图谱抽取流程中的一个组件。
         * 你的任务是从给定数据中抽取并定义一组知识图谱用的关系（三元组）。输出必须是有效的 JSON 对象。
         *
         * 请根据提供的文本和上下文，抽取相关的三元组关系，并以 JSON 格式返回。
         *
         * 约束条件：
         * 1. 所有抽取出的三元组**必须**遵循 [主体, 关系, 客体] 的格式（a --b-> c）。
         * 2. 每个三元组的主体**必须**是来自给定实体列表中的一个。如果发现同义词或指向同一个事物，请使用给定的实体名称来代替。
         * 3. 关系（谓词）应该清楚地表达主体和客体之间的联系。请保持其简洁明了，避免任何描述性或形容性的词语。
         * 4. 三元组必须是有意义的，并且能为知识图谱贡献有价值的信息。请舍弃任何无意义的关系。
         * 5. 谓词尽可能使用中文 zh_CN 书写
         *
         * 给定实体（请用这些实体作为三元组的主体）：
         * $givenEntities
         *
         * 上下文主题（仅抽取与此主题相关的关系）：
         * $context
         *
         * 输出示例：
         * ${getExample()}
         *
         * 注意事项：
         * 1. 输出非格式化的JSON以节省token。
         * 2. 不要翻译原始数据中的任何词语，保留其原始语言（处理字符串值时）。
         *
         * 待处理数据：
         * $data
         */
        return """
You are a component in a knowledge graph extraction pipeline.
Your task is to extract and define a set of relations (triples) for a knowledge graph from the given data. The output must be a valid JSON object.

Based on the provided text and context, extract relevant triple relations and return them in JSON format.

Constraints:
1. All extracted triples **MUST** adhere to the [subject, predicate, object] format (e.g., a --b-> c).
2. The subject of each triple **MUST** be an entity from the provided list. If you find synonyms or references to the same thing, use the given entity name as a substitute.
3. The predicate (relation) should clearly express the connection between the subject and object. Keep it concise and simple, avoiding any descriptive or adjectival words.
4. Triples must be meaningful and contribute valuable information to the knowledge graph. Discard any meaningless relations.
5. Predicates should be in Chinese (zh_CN) whenever possible.

Given Entities (Use these entities as subjects for your triples):
$givenEntities

Contextual Topic (Only extract relations relevant to this topic):
$context

Example Output:
${getExample()}

Notes:
1. Output unformatted JSON to save tokens.
2. DO NOT translate any words in the raw data; keep them in their original language (when processing string values).

Data to Process:
$data
    """.trimIndent()
    }

    private fun getExample(): String {
        return prettyGson.toJson(
            Relations(
                listOf(
                    listOf("Tom", "EAT", "apple"),
                    listOf("王明", "父亲是", "王浩")
                )
            )
        )
    }

    fun extract(givenEntities: List<String>, context: String, data: String): Relations {
        val givenEntitiesStr = prettyGson.toJson(givenEntities)
        return extract(givenEntitiesStr, context, data)
    }
}