package interfaces

import beans.ExtractedData
import iLLMJsonExtractorExample

/**
 * @sample iLLMJsonExtractorExample
 */
interface ILLMJsonExtractor {
    fun createPrompt(input: String): String
    fun extract(input: String): ExtractedData
}