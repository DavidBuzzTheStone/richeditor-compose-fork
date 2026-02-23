internal fun removeHtmlTextExtraSpaces(input: String, trimStart: Boolean = false): String {
    return input
        .replace(' ', ' ')
        .replace('\n', ' ')
        .replace("\\s+".toRegex(), " ")
        .let {
            if (trimStart)
                it.trimStart()
            else
                it
        }
}
private fun main() {
    val res1 = removeHtmlTextExtraSpaces(" second", true)
    println("res1: '$res1'")
}
