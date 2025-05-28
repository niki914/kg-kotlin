package utils

import beans.Api
import beans.NodeJsonData
import getNodePrompt
import utils.baseclasses.JsonExtractor4Openai

class NodeExtractor(
    apiKeys: List<String>,
    baseUrl: String,
    model: String
) : JsonExtractor4Openai<NodeJsonData>(apiKeys, baseUrl, model) {
    constructor(api: Api) : this(
        api.apiKeys,
        api.baseUrl,
        api.modelName
    )

    /**
     * context, data
     */
    override fun createPrompt(vararg input: String?): String {
        val context = tryGetOrNull { input[0] } ?: "[没有明确的主题, 进行宽泛的抽取]"
        val classes = tryGetOrNull { input[1] } ?: "[没有明确的类, 进行宽泛的抽取, 返回时返回空的类, 其他属性正常抽取]"
        val data = tryGetOrNull { input[2] } ?: "[如果你看见这个, 说明没有待处理数据, 直接返回空 json 体即可]"
        return getNodePrompt(context, classes, data)
    }

    override fun extract(vararg input: String?): NodeJsonData {
        val json = extractJsonString(*input)
        val nodeJsonData = json.asJsonClass<NodeJsonData>()
        return nodeJsonData
    }
}