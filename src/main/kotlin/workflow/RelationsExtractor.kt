package workflow

import Api
import beans.Relations
import utils.tryGetOrNull
import workflow.base.classes.JsonExtractor4Openai

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
         * 随时变动，请以英文版本为准
         *
         * 你是知识图谱抽取流程中的一个组件。
         * 你的任务是从给定数据中抽取并定义一组知识图谱用的关系（三元组）。输出必须是有效的 JSON 对象。
         *
         * 请根据提供的文本和上下文，抽取相关的三元组关系，并以 JSON 格式返回。
         *
         * 严格约束条件：
         * 1. 所有抽取出的三元组**必须**遵循 [主体, 关系, 客体] 的格式（a --b-> c）。
         * 2. 每个三元组的主体和客体中，**至少有一个**必须来自给定实体列表。如果发现同义词或指向同一事物的表达，请使用给定的实体名称替代。
         * 3. 关系（谓词）应该清楚地表达主体和客体之间的联系。保持简洁明了，避免任何描述性或修饰性词语。
         * 4. 三元组必须是有意义的，并且能为知识图谱贡献有价值的信息。请舍弃任何无意义的关系。
         * 5. 谓词尽可能使用中文 zh_CN 表达
         * 6. 输出非格式化的JSON以节省token。
         * 7. 不要翻译原始数据中的任何词语，保留其原始语言（处理字符串值时）。
         * 8. 确保不**给三元组中的实体提供含糊的名称**，比如"论文"，应该提供确切的论文标题，否则将严重影响图谱质量。
         *
         * 给定实体（三元组的主体和客体中至少有一个必须来自此列表）：
         * $givenEntities
         *
         * 上下文主题（仅抽取与此主题相关的关系）：
         * $context
         *
         * 输出示例：
         * ${getExample()}
         *
         * 待处理数据：
         * $data
         */
        return """
# You are a component in a knowledge graph extraction pipeline.

Your task is to extract and define a set of relations (triples) for a knowledge graph from the given data. The output must be a valid JSON object.

Based on the provided text and context, extract relevant triple relations and return them in JSON format.

# Strict Constraints:
1. All extracted triples **MUST** follow the [subject, predicate, object] format (e.g., a --b-> c).
2. **At least one** of the subject or object in each triple **MUST** be an entity from the provided list. If you find synonyms or references to the same entity, use the given entity name as a substitute.
3. The predicate (relation) should clearly express the connection between the subject and object. Keep it concise and straightforward, avoiding any descriptive or adjectival words.
4. Triples must be meaningful and contribute valuable information to the knowledge graph. Discard any meaningless relations.
5. Predicates should be expressed in Chinese (zh_CN) whenever possible.
6. Output unformatted JSON to save tokens.
7. DO NOT translate any words in the raw data; keep them in their original language (when processing string values).
8. Ensure you **don't give entities in the triples vague names** like "论文" - always use the exact thesis title instead, otherwise it will severely impact graph quality.
9. Before you start answering, consider combining some of the triples that you think are highly similar to avoid redundancy.

## Given Entities

At least one of the subject or object in each triple must come from this list:
$givenEntities

## Contextual Topic

Only extract relations relevant to this topic:
$context

## Example Output:
${getExample()}

## Data to Process:
$data
    """.trimIndent()
    }

    private fun getExample(): String {
        return gson.toJson(
            Relations(
                listOf(
                    listOf("Tom", "EAT", "apple"),
                    listOf("王明", "父亲是", "王浩")
                )
            )
        )
    }

    fun extract(givenEntities: List<String>, context: String, data: String): Relations {
        val givenEntitiesStr = gson.toJson(givenEntities)
        return extract(givenEntitiesStr, context, data)
    }
}