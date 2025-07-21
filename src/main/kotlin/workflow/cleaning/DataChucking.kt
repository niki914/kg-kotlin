package workflow.cleaning

import beans.GroupedItems
import workflow.base.interfaces.IDataChucking
import kotlin.math.ceil

class DataChucking : IDataChucking {
    override fun formatToChuckedStrings(chuckSize: Long, groupedItems: GroupedItems): List<String> {
        if (chuckSize <= 0) return emptyList()

        val result = mutableListOf<String>()
        val currentChunk = StringBuilder()
        var currentSize = 0L

        // 过滤 Formula 和 NarrativeText 的 text 字段
        val strings = groupedItems.items.map { it.text }

        // 处理每条数据
        val pendingItems = ArrayDeque(strings)
        while (pendingItems.isNotEmpty()) {
            val item = pendingItems.removeFirst()
            val itemBytes = item.toByteArray(Charsets.UTF_8)
            val itemSize = itemBytes.size.toLong()

            // 检查单条数据是否需要截断
            if (itemSize > chuckSize) {
                // 截断到 chuckSize
                val maxChars = estimateMaxChars(item, chuckSize)
                val truncated = item.take(maxChars)
                val truncatedBytes = truncated.toByteArray(Charsets.UTF_8)
                val truncatedSize = truncatedBytes.size.toLong()

                // 确保截断后不超过 chuckSize
                if (truncatedSize <= chuckSize) {
                    // 剩余部分追加前 size*10% 内容
                    val remaining = item.drop(maxChars)
                    if (remaining.isNotEmpty()) {
                        val prefixSize = ceil(chuckSize * 0.1).toInt()
                        val prefix = truncated.take(prefixSize.coerceAtMost(truncated.length))
                        pendingItems.addFirst(prefix + remaining)
                    }

                    // 尝试加入当前分块
                    if (currentSize + truncatedSize + (if (currentChunk.isNotEmpty()) 1 else 0) <= chuckSize) {
                        if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                        currentChunk.append(truncated)
                        currentSize += truncatedSize + (if (currentChunk.isNotEmpty()) 1 else 0)
                    } else {
                        // 当前分块已满，保存并创建新分块
                        if (currentChunk.isNotEmpty()) {
                            result.add(currentChunk.toString())
                            currentChunk.clear()
                            currentSize = 0L
                        }
                        // 截断数据直接作为新分块
                        currentChunk.append(truncated)
                        currentSize = truncatedSize
                    }
                }
                continue
            }

            // 正常数据，检查是否可以加入当前分块
            val additionalSize = itemSize + (if (currentChunk.isNotEmpty()) 1 else 0)
            if (currentSize + additionalSize <= chuckSize) {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                currentChunk.append(item)
                currentSize += additionalSize
            } else {
                // 当前分块已满，保存并创建新分块
                if (currentChunk.isNotEmpty()) {
                    result.add(currentChunk.toString())
                    currentChunk.clear()
                    currentSize = 0L
                }
                // 新分块直接加入当前数据
                currentChunk.append(item)
                currentSize = itemSize
            }
        }

        // 添加最后一个分块（如果非空）
        if (currentChunk.isNotEmpty()) {
            result.add(currentChunk.toString())
        }

        return result
    }

    // 估算最多可取的字符数，使字节数不超过 limit
    private fun estimateMaxChars(text: String, limit: Long): Int {
        var byteCount = 0L
        for (i in text.indices) {
            val charBytes = text[i].toString().toByteArray(Charsets.UTF_8)
            if (byteCount + charBytes.size > limit) {
                return i
            }
            byteCount += charBytes.size
        }
        return text.length
    }
}