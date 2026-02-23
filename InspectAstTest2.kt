import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser
import com.mohamedrejeb.richeditor.model.RichTextState

fun main() {
    val html = """
        <!DOCTYPE html>
        <html>
        <body>

        <div style="background-color: red">
            <br>
        </div>
        
        <p>
            dd
            <b>
                <i>
                    dd
                </i>
            </b>
            <inline id="foo"/>
        </p>
        
        <p>
            second
        </p>

        </body>
        </html>
    """.trimIndent()
    val state = RichTextStateHtmlParser.encode(html)
    println("P size: " + state.richParagraphList.size)
    println("P[0] ch.size: " + state.richParagraphList[0].children.size)
    if (state.richParagraphList[0].children.size > 0) {
        println("P[0] ch[0] text: '" + state.richParagraphList[0].children[0].text + "'")
    }
}
