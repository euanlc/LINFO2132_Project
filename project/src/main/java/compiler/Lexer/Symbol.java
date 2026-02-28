package compiler.Lexer;

import java.lang.String;

public class Symbol {
    private String type;
    private String value;

    public Symbol(String type, String value) {
        this.type = type;
        this.value = value;
    }

    // Overiding the toString method to return the type and value of the symbol as <TYPE, value>
    @Override
    public String toString() {
        return "<" + type + ", " + value + ">";
    }
}
