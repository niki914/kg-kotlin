package utils.workflow.base.interfaces

import beans.GroupedItems
import beans.Item

interface ICleanDataParser {
    /**
     * 读取文件为 Item 对象
     *
     * 如: "c:\test.pdf"
     */
    fun readFromPath(absolutePath: String): List<Item>

    /**
     * 按 metadata.filename 分组, 返回的 groupedItem 中用转化后的输出路径, 如 test.pdf -> test_pdf.json, Dockerfile -> Dockerfile.json .txt -> txt_${四位简单哈希}.json
     */
    fun groupByName(items: List<Item>): List<GroupedItems>
}