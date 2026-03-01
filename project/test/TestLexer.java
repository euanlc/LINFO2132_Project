package compiler.Lexer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import java.io.StringReader;

public class TestLexer {

    @Test
    public void testLexerBasic() {
        // A very simple input to ensure Lexer reads Types and Identifiers
        String input = "INT x = 10;";
        StringReader reader = new StringReader(input);
        Lexer lexer = new Lexer(reader);

        // 1. Check if first token is INT (TYPE)
        Symbol s1 = lexer.getNextSymbol();
        assertNotNull(s1);
        assertTrue(s1.toString().contains("TYPE"));

        // 2. Check if second token is x (IDENTIFIER)
        Symbol s2 = lexer.getNextSymbol();
        assertNotNull(s2);
        assertTrue(s2.toString().contains("IDENTIFIER"));

        System.out.println("Basic Lexer test passed!");
    }
}
