fun main() {
    try {
        println("Result of empty substring: '" + "".substring(0, 1) + "'")
    } catch(e: Exception) {
        println("Caught exception: $e")
    }
}
