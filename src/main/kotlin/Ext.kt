fun Throwable.toLogString(): String {
    return "message: ${message}\ncause: ${cause}\nstack trace: ${stackTraceToString()}"
}

fun Throwable.println() {
    println("错误:\n${toLogString()}")
}