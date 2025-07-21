package utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.awt.Desktop
import java.io.File
import java.util.*

val prettyGson: Gson by lazy {
    GsonBuilder().setPrettyPrinting().create()
}

fun <T> tryGetOrNull(block: () -> T): T? = try {
    block()
} catch (t: Throwable) {
    t.logE(true)
    null
}

fun Throwable.toSimpleLogString(): String {
    return "message: ${message}\ncause: $cause"
}

fun Throwable.toLogString(): String {
    return "message: ${message}\ncause: ${cause}\nstack trace: ${stackTraceToString()}"
}

fun Throwable.logE(simpleLog: Boolean = false, tag: String = "") {
    val log = if (simpleLog) toSimpleLogString() else toLogString()

    logE("\n" + log, tag)
}

fun writeStringToFile(parent: String, child: String, content: String) {
    File(parent).mkdirs()
    val outputFile = File(parent, child)
    outputFile.writeText(content)
}

fun openFolder(path: String): Boolean = try {
    val folder = File(path)
    if (!folder.exists() || !folder.isDirectory) {
        false
    } else {
        Desktop.getDesktop().open(folder)
        true
    }
} catch (_: Throwable) {
    false
}

fun logV(
    msg: String = "",
    tag: String = ""
) = Log.v(tag, msg)

fun logD(
    msg: String = "",
    tag: String = ""
) = Log.d(tag, msg)

fun logI(
    msg: String = "",
    tag: String = ""
) = Log.i(tag, msg)

fun logW(
    msg: String = "",
    tag: String = ""
) = Log.w(tag, msg)

fun logE(
    msg: String = "",
    tag: String = "",
    throwable: Throwable? = null
) = Log.e(tag, msg, throwable)

private val scanner = Scanner(System.`in`)

fun pause(msg: String = "按任意键继续...") {
    scanner.apply {
        logE(msg, "暂停")
        nextLine()
    }
}