package compiler.Lexer;

import java.lang.String;

public class Symbol<T> {
    private String type;
    private T value;

    public Symbol(String type, T value) {
        this.type = type;
        this.value = value;
    }

    // Overiding the toString method to return the type and value of the symbol as <TYPE, value>
    @Override
    public String toString() {
        return "<" + type + ", " + value + ">";
    }
}
