import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser
fun main() {
    val html = """
        <!DOCTYPE html>
        <html>
        <body>

        <h1>The img element</h1>

        <img src="https://picsum.photos/200/300" alt="Girl in a jacket" width="500" height="600">

        </body>
        </html>
    """.trimIndent()
    val state = RichTextStateHtmlParser.encode(html)
    println("P0 size: " + state.richParagraphList[0].children.size)
    println("P0 ch0: " + state.richParagraphList[0].children[0].text)
    if(state.richParagraphList[0].children.size > 1) {
       println("P0 ch1 text: " + state.richParagraphList[0].children[1].text + " | spanStyle: " + state.richParagraphList[0].children[1].richSpanStyle)
    }
}
