package workflow.base.interfaces

import beans.GroupedItems

/**
 * 处理数据分块
 */
interface IDataChucking {
    // 将分组好的元素再次处理成每段大小适中的字符串列表, 确保数据没有过大
    fun formatToChuckedStrings(
        chuckSize: Long,
        groupedItems: GroupedItems
    ): List<String>
}