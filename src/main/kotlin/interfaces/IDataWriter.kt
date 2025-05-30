package interfaces

import beans.ExtractedData

interface IDataWriter {
    fun writeData(data: ExtractedData)
}