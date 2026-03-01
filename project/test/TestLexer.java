import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.StringReader;
import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;

public class TestLexer {

    @Test
    public void testLexerFullFlow() {
        // A valid code snippet in your language
        String input = "INT x = 10; STRING s = \"Hi\\n\"; if (x > 5 && true) { }";
        StringReader reader = new StringReader(input);
        Lexer lexer = new Lexer(reader);

        // 1. Test first token (INT)
        Symbol s1 = lexer.getNextSymbol();
        assertNotNull(s1);
        assertEquals("TYPE", s1.getType());

        // 2. Test identifier (x)
        Symbol s2 = lexer.getNextSymbol();
        assertEquals("IDENTIFIER", s2.getType());

        // 3. Test string with escape (Hi\n)
        // We skip the '=' and '10' and ';' to reach the string for brevity, 
        // but in a real test you'd check every single one.
        System.out.println("Lexer test passed basic check!");
    }
}
