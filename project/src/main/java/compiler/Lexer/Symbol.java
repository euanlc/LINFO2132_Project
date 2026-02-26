package compiler.Lexer;

// TODO: define symbol types
//       decide how to represent value (just string maybe?)

public class Symbol {
    private String type;
    private String value;

    private String IdentifyType(String value) {
        if (value.equals("NULL")) {
            return "NULL";
        }
        // Add more type identification logic here
        return "UNKNOWN";
    }

    public Symbol(String value) {
        this.type = IdentifyType(value);
        this.value = value;
    }

    // Overiding the toString method to return the type and value of the symbol as <TYPE, value>
    @Override
    public String toString() {
        return "<" + type + ", " + value + ">";
    }
}
