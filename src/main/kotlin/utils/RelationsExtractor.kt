package utils

import beans.Api
import beans.RelationJsonData
import getRelationPrompt
import utils.baseclasses.JsonExtractor4Openai

class RelationsExtractor(
    apiKeys: List<String>,
    baseUrl: String,
    model: String
) : JsonExtractor4Openai<RelationJsonData>(apiKeys, baseUrl, model) {
    constructor(api: Api) : this(
        api.apiKeys,
        api.baseUrl,
        api.modelName
    )

    /**
     * given nodes, context, data
     */
    override fun _createPrompt(vararg input: String?): String {
        val givenNodes = tryGetOrNull { input[0] } ?: "[没有给定节点, 节点现在由你自行定义]"
        val context = tryGetOrNull { input[1] } ?: "[没有明确的主题, 进行宽泛的抽取]"
        val data = tryGetOrNull { input[2] } ?: "[如果你看见这个, 说明没有待处理数据, 直接返回空 json 体即可]"
        return getRelationPrompt(givenNodes, context, data)
    }

    override fun extract(vararg input: String?): RelationJsonData {
        val json = extractJsonString(*input)
        val relationJsonData = json.asJsonClass<RelationJsonData>()
        return relationJsonData
    }
}