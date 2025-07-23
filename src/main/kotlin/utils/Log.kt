package utils

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

/**
 * 日志系统，在 terminal 中带有颜色
 */
object Log {
    var level: Level = Level.VERBOSE

    enum class Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        NONE
    }

    init {
        AnsiConsole.systemInstall() // 启用 Jansi
    }

    private fun formatMessage(level: String, tag: String = "", msg: String, throwable: Throwable? = null): String {
        val ansi = Ansi.ansi()
        val colored = when (level) {
            "ERROR" -> ansi.fgRed()
            "DEBUG" -> ansi.fgGreen()
            "INFO" -> ansi.fgBlue()
            "WARN" -> ansi.fgYellow()
            else -> ansi
        }

        val trace = throwable?.stackTraceToString() ?: ""
        val t = tag.ifBlank { level }
        return colored.a("$t: $msg${if (trace.isNotEmpty()) "\n$trace" else ""}").reset().toString()
    }

    fun v(tag: String = "", msg: String) {
        if (level > Level.VERBOSE) return
        println(formatMessage("VERBOSE", tag, msg))
    }

    fun d(tag: String = "", msg: String) {
        if (level > Level.DEBUG) return
        println(formatMessage("DEBUG", tag, msg))
    }

    fun i(tag: String = "", msg: String) {
        if (level > Level.INFO) return
        println(formatMessage("INFO", tag, msg))
    }

    fun w(tag: String = "", msg: String, throwable: Throwable? = null) {
        if (level > Level.WARNING) return
        println(formatMessage("WARN", tag, msg, throwable))
    }

    fun e(tag: String = "", msg: String, throwable: Throwable? = null) {
        if (level > Level.ERROR) return
        println(formatMessage("ERROR", tag, msg, throwable))
    }
}