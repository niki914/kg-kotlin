import beans.GroupedItems
import config.DEEPSEEK_KEYs
import config.INPUT_PATH
import interfaces.ICleanDataParser
import interfaces.IDataChucking
import interfaces.ILLMJsonExtractor

fun iCleanDataParserExample() {
    val parser: ICleanDataParser = CleanDataParser()

    try {
        // 读取数据
        val items = parser.readFromPath(INPUT_PATH)
        // 分组
        val groupedItemsList: List<GroupedItems> = parser.groupByName(items)
    } catch (e: Exception) {
        e.println()
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
    val extractor: ILLMJsonExtractor = object : JsonExtractor(
        DEEPSEEK_KEYs[0],
        "https://api.deepseek.com/",
        "deepseek-chat"
    ) {
        override fun createPrompt(input: String): String {
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
    }

    val data =
        extractor.extract("为了规范我校国家社会科学基金（以下简称国家 社科基金）以及教育部高等学校哲学社会科学研究项目（以下 简称教育部哲社项目）经费的使用和管理，充分调动我校教师 参与科研活动的积极性，根据《国家社会科学基金项目资金管 理办法》（财教〔2021〕237号）、《高等学校哲学社会科学繁 荣计划专项资金管理办法》（财教〔2021〕285号），以及广东 省哲学社会科学规划领导小组办公室等上级主管部门的相关要 求，结合我校实际，特制定本办法。")

    // data.toJson (用 gson) -> write json to file ${groupedItems.filename}
    println(data.toString())
}