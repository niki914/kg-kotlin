package interfaces

import beans.GroupedItems
import iDataChuckingExample

/**
 * @sample iDataChuckingExample
 */
interface IDataChucking {
    // 确保数据没有过大
    fun formatToChuckedStrings(
        chuckSize: Long,
        groupedItems: GroupedItems
    ): List<String>
}