package utils

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

object Log {
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

    fun e(tag: String = "", msg: String, throwable: Throwable? = null) {
        println(formatMessage("ERROR", tag, msg, throwable))
    }

    fun d(tag: String = "", msg: String) {
        println(formatMessage("DEBUG", tag, msg))
    }

    fun i(tag: String = "", msg: String) {
        println(formatMessage("INFO", tag, msg))
    }

    fun w(tag: String = "", msg: String, throwable: Throwable? = null) {
        println(formatMessage("WARN", tag, msg, throwable))
    }

    fun v(tag: String = "", msg: String) {
        println(formatMessage("VERBOSE", tag, msg))
    }
}