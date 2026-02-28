package compiler.Lexer;

import java.util.List;
import java.util.ArrayList;
import java.lang.String;

import java.io.Reader;
import compiler.Lexer.Symbol;

//TODO: define language
//      check if char is in language, if not throw an error
//      identify symbol type

// Language definition:
// 1. Identifiers: [a-z_][a-zA-Z0-9_]*
// 2. Collections: [A-Z][a-zA-Z0-9_]*

public class Lexer {
    private Reader input;
    private static final String SPECIAL_CHARACTERS = "=+-*/%<>(){}[].,;|&"; // Define special characters
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
                ch = (char) input.read();
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
                    } else if (SPECIAL_CHARACTERS.indexOf(ch) != -1) {
                        state = 4; // Special symbol state
                        buffer += ch;
                    } else if (ch == '"'){
                        state = 5; // String literal state
                    } else {
                        throw new IllegalArgumentException("unrecognised token: " + ch);
                    }
                } else if (state == 1) { // Identifier state
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ch;
                        return new Symbol("IDENTIFIER", buffer); // end of symbol
                    }
                } else if (state == 2) { // Collection state
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ch;
                        return new Symbol("COLLECTION", buffer); // end of symbol
                    }
                } else if (state == 3) { // Number state
                    if (Character.isDigit(ch) || ch == '.') {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ch;
                            return new Symbol("NUMBER", buffer); // end of symbol
                    }
                } else if (state == 4) { // Special symbol state
                    if (SPECIAL_CHARACTERS.indexOf(ch) != -1) {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ch;
                        return new Symbol("SPECIAL_SYMBOL", buffer); // end of symbol
                    }
                } else if (state == 5) { // String literal state
                    if (ch != '"') {
                        buffer += ch;
                    } else {
                        this.prevEndingChar = ' '; // avoid treating the closing quote as a separate opening quote for the next symbol
                        return new Symbol("STRING", buffer); // end of symbol
                    }
                }
                ch = (char) input.read();
            }
            return new Symbol("EOF", ""); // End of file symbol
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
