import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser

fun main() {
    val html = "<!DOCTYPE html><html><body>\n\n<h1>The img element</h1>\n\n<img src=\"foo.jpg\">\n\n</body></html>"
    val state = RichTextStateHtmlParser.encode(html)
    println("Paragraphs size: " + state.richParagraphList.size)
    for(i in state.richParagraphList.indices) {
        val p = state.richParagraphList[i]
        println("P" + i + " children size: " + p.children.size)
        for(j in p.children.indices) {
            val c = p.children[j]
            println("  P" + i + " C" + j + ": text='" + c.text + "', style=" + c.richSpanStyle)
        }
    }
}
