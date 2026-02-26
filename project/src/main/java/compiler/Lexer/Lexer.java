package compiler.Lexer;
import java.io.Reader;
import Symbol from compiler.Lexer.Symbol;

//TODO: define language
//      check if char is in language, if not throw an error
//      identify symbol type

public class Lexer {
    private Reader input;
    public Lexer(Reader input) {
        this.input = input;
    }
    private String SimpleMatch(){
        int state = 0; // Initial state
        List<Character> buffer = new ArrayList<>(); // Buffer to hold characters for the current symbol
        while(true) {
            char nextChar = (char) input.read();
            if (nextChar == -1) {
                break; // End of file
            }
            if (state == 0) {
                // Write out NFA logic here, add to buffer if transition is valid, throw error if not
            }
        }
        return new String(buffer.toArray(new Character[0])); // Convert buffer to string and return
    }

    public Symbol getNextSymbol() {
        // if nextchar is whitespace, skip it
        // if nextchar is # skip all characters until next eol
        char ch = (char) input.read();
        if (Character.isWhitespace(ch)) {
            // Skip whitespace
            return getNextSymbol();
        }
        if (ch == '#') {
            // Skip comment until end of line
            while ((ch = (char) input.read()) != '\n' && ch != -1) {
                // Continue skipping until newline or end of file
            }
            return getNextSymbol(); // Return next symbol after comment
        }
        return new Symbol("TEST");
    }
}
