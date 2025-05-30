import beans.Api
import beans.ExtractedData
import beans.GroupedItems
import interfaces.ICleanDataParser
import interfaces.IDataChucking
import utils.BaseLLMJsonExtractor
import utils.CleanDataParser
import utils.DataChucking
import utils.baseclasses.JsonExtractor4Openai
import utils.logE

val testData = ExtractedData(
    edges = listOf("制定", "规范", "负责", "监督", "使用", "包含"),
    nodes = listOf(
        "广东工业大学财务",
        "国家社会科学基金及教育部高等学校哲学社会科学研究项目经费管理办法",
        "国家社科基金及教育部哲社项目资金管理",
        "学校",
        "项目负责人",
        "业务费"
    ),
    relations = listOf(
        listOf("广东工业大学财务", "制定", "国家社会科学基金及教育部高等学校哲学社会科学研究项目经费管理办法"),
        listOf(
            "国家社会科学基金及教育部高等学校哲学社会科学研究项目经费管理办法",
            "规范",
            "国家社会科学基金及教育部高等学校哲学社会科学研究项目经费的使用和管理"
        ),
        listOf("学校", "负责", "国家社科基金及教育部哲社项目资金管理"),
        listOf("学校", "监督", "国家社科基金及教育部哲社项目资金管理"),
        listOf("项目负责人", "使用", "国家社科基金及教育部哲社项目资金"),
        listOf("项目负责人", "负责", "国家社科基金及教育部哲社项目资金合规性"),
        listOf("业务费", "包含", "图书购置"),
        listOf("业务费", "包含", "资料收集"),
        listOf("业务费", "包含", "复印翻拍"),
        listOf("业务费", "包含", "文献检索"),
        listOf("业务费", "包含", "数据采集"),
        listOf("业务费", "包含", "资料翻译"),
        listOf("业务费", "包含", "印刷出版"),
        listOf("业务费", "包含", "会议差旅"),
        listOf("业务费", "包含", "国际合作与交流"),
    )
)

fun iCleanDataParserExample() {
    val parser: ICleanDataParser = CleanDataParser()

    try {
        // 读取数据
        val items = parser.readFromPath(INPUT_PATH)
        // 分组
        val groupedItemsList: List<GroupedItems> = parser.groupByName(items)
    } catch (e: Exception) {
        e.logE()
    }
}

fun iDataChuckingExample() {
    val chucking: IDataChucking = DataChucking()
    val items = GroupedItems(
        "a.txt",
        listOf()
    )
    val strings = chucking.formatToChuckedStrings(2048L, items)
    strings.forEach(::println)
}

fun iLLMJsonExtractorExample() {
    val extractor: BaseLLMJsonExtractor<ExtractedData> = object : JsonExtractor4Openai<ExtractedData>(Api.Deepseek) {
        override fun createPrompt(vararg input: String?): String {
            return """
            Extract the edges and nodes from the input text and return a JSON object with two fields:
            "edges" (list of strings) and "nodes" (list of strings). Ensure the output is valid JSON.

            Input: $input

            Example output:
            {
                "edges": ["edge1", "edge2"],
                "nodes": ["node1", "node2"]
            }

            Return only the JSON object, no additional text.
        """.trimIndent()
        }

        override fun extract(vararg input: String?): ExtractedData {
            val json = extractJsonString(*input)
            val extractedData = json.asJsonClass<ExtractedData>()
            return extractedData
        }
    }

    val data =
        extractor.extract("为了规范我校国家社会科学基金（以下简称国家 社科基金）以及教育部高等学校哲学社会科学研究项目（以下 简称教育部哲社项目）经费的使用和管理，充分调动我校教师 参与科研活动的积极性，根据《国家社会科学基金项目资金管 理办法》（财教〔2021〕237号）、《高等学校哲学社会科学繁 荣计划专项资金管理办法》（财教〔2021〕285号），以及广东 省哲学社会科学规划领导小组办公室等上级主管部门的相关要 求，结合我校实际，特制定本办法。")

    // data.toJson (用 gson) -> write json to file ${groupedItems.filename}
    println(data.toString())
}