package compiler.Lexer;

import java.util.List;
import java.util.ArrayList;
import java.lang.String;
import java.util.Set;

import java.io.Reader;
import compiler.Lexer.Symbol;

//TODO: define language


// Language definition:
// 1. Identifiers: [a-z_][a-zA-Z0-9_]*
// 2. Collections: [A-Z][a-zA-Z0-9_]*
// 3. Keywords: final, coll, def, for, while, if, else, return, not, ARRAY
// 4. Types: INT, FLOAT, STRING, BOOLEAN

public class Lexer {
    private Reader input;
    private static final Set<String> OPERATORS = Set.of("=", "+", "-", "*", "/", "%", "==", "=/=", "<", ">", "<=", ">=", "&&", "||");
    private static final Set<String> SPECIAL_CHARACTERS = Set.of("(", ")", "{", "}", "[", "]", ".", ",", ";");
    private static final Set<String> KEYWORDS = Set.of("final", "coll", "def", "for", "while", "if", "else", "return", "not", "ARRAY"); 
    private static final Set<String> TYPES = Set.of("INT", "FLOAT", "STRING", "BOOL");
    private static final Set<String> BOOLEAN = Set.of("true", "false");
    private char prevEndingChar = ' '; // To track the last character that ended a symbol

    public Lexer(Reader input) {
        this.input = input;
    }
    
    public Symbol getNextSymbol() {
        // if ch is whitespace, skip it
        // if ch is # skip all characters until next eol
        try { 
            char ch;
            if (prevEndingChar == ' ' || Character.isWhitespace(prevEndingChar)) {
                ch = (char) this.input.read();
            } else {
                ch = prevEndingChar; // Start with the last character that ended a symbol
            }

            if (Character.isWhitespace(ch)) {
                // Skip whitespace
                return getNextSymbol();
            }

            if (ch == '#') {
                // Skip comment until end of line
                while ((ch = (char) input.read()) != '\n' && ch != (char) -1) {
                    // Continue skipping until newline or end of file
                }
                return getNextSymbol(); // Return next symbol after comment
            }

            int state = 0; // Initial state
            String buffer = ""; // Buffer to hold characters for the current symbol
            while(true) {
                // System.out.println("Current char: " + ch + ", State: " + state + ", Buffer: '" + buffer + "'");

                if (ch == (char) -1) {
                    break; // End of file
                }
                if (state == 0) {
                    if (Character.isLowerCase(ch) || ch == '_') {
                        state = 1; // Identifier state
                        buffer += ch;
                    } else if (Character.isUpperCase(ch)) {
                        state = 2; // Collection state
                        buffer += ch;
                    } else if (Character.isDigit(ch) || ch == '.') {
                        state = 3; // Number state
                        buffer += ch;
                    } else if (OPERATORS.contains(String.valueOf(ch)) || ch == '&' || ch == '|') {
                        state = 4; // Special symbol state
                        buffer += ch;
                    } else if (ch == '"'){
                        state = 5; // String literal state
                    } else if (SPECIAL_CHARACTERS.contains(String.valueOf(ch))) {
                        state = 6;
                        buffer += ch;
                    } else {
                        throw new IllegalArgumentException("unrecognised token: " + ch);
                    }
                } else if (state == 1) { // Identifier state
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ch;
                        if (KEYWORDS.contains(buffer)) {
                            return new Symbol<String>("KEYWORD", buffer); // end of symbol
                        } else if (BOOLEAN.contains(buffer)) {
                            return new Symbol<Boolean>("BOOLEAN", Boolean.parseBoolean(buffer)); // end of symbol
                        } else {
                            return new Symbol<String>("IDENTIFIER", buffer); // end of symbol
                        }
                    }
                } else if (state == 2) { // Collection state
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ch;
                        if (buffer.equals("ARRAY")) {
                            return new Symbol<String>("KEYWORD", buffer); // end of symbol
                        } else  if (TYPES.contains(buffer)) {
                            return new Symbol<String>("TYPE", buffer); // end of symbol
                        } else {
                            return new Symbol<String>("COLLECTION", buffer); // end of symbol
                        }
                    }
                } else if (state == 3) { // Number state
                    if (Character.isDigit(ch) || ch == '.') {
                        buffer += ch;
                    } else if (buffer.equals(".")) {
                        this.prevEndingChar = ch;
                        return new Symbol<String>("SPECIAL_CHARACTER", buffer); // treat . as special character if it's alone
                    } else {
                        this.prevEndingChar = ch;
                        if (buffer.contains(".")) {
                            return new Symbol<Float>("FLOAT", Float.parseFloat(buffer)); // end of symbol
                        } else {
                            return new Symbol<Integer>("INT", Integer.parseInt(buffer)); // end of symbol
                        }
                    }
                } else if (state == 4) { // Special symbol state
                    if (OPERATORS.contains(String.valueOf(buffer + ch)) || String.valueOf(buffer + ch).equals("=/")) {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ch;
                        return new Symbol<String>("OPERATOR", buffer); // end of symbol
                    }
               } else if (state == 5) { // String literal state
    if (ch == '\\') { 
        // If we encounter a backslash, read the next character for the escape sequence
        char nextCh = (char) input.read(); 
        if (nextCh == 'n') {
            buffer += '\n';
        } else if (nextCh == '\\') {
            buffer += '\\';
        } else if (nextCh == '"') {
            buffer += '"';
        } else {
            // If it's an unrecognized escape character, append both to the buffer
            buffer += '\\';
            buffer += nextCh;
        }
    } else if (ch != '"') {
        buffer += ch;
    } else {
        this.prevEndingChar = ' '; // avoid treating the closing quote as a separate opening quote for the next symbol
        return new Symbol<String>("STRING", buffer); // end of symbol
    }
} else if (state == 6) { // Special symbol state
                    this.prevEndingChar = ch;
                    return new Symbol<String>("SPECIAL_CHARACTER", buffer); // end of symbol
                }
                ch = (char) input.read();
            }
            return new Symbol<String>("EOF", ""); // End of file symbol
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
