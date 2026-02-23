import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser
import com.mohamedrejeb.richeditor.model.RichTextState

fun main() {
    val text = "<p> dd</p>"
    val state = RichTextStateHtmlParser.encode(text)
    
    val p = state.richParagraphList[0]
    println("P0 isBlank? " + p.isBlank())
    println("P0 children size: " + p.children.size)
    for(i in p.children.indices) {
        println("Child $i: '${p.children[i].text}'")
        println("Child $i isBlank? " + p.children[i].isBlank())
    }
}
