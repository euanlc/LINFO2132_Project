package compiler.Lexer;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

public class Lexer {
    private Reader input;
    private int currentChar;

    private static final Set<String> KEYWORDS = Set.of("final", "coll", "def", "for", "while", "if", "else", "return", "not", "ARRAY");
    private static final Set<String> TYPES = Set.of("INT", "FLOAT", "STRING", "BOOL", "BOOLEAN");
    private static final Set<String> OPERATORS = Set.of("=", "+", "-", "*", "/", "%", "==", "=/=", "<", ">", "<=", ">=", "&&", "||", "->");
    private static final Set<String> SPECIAL_CHARACTERS = Set.of("(", ")", "{", "}", "[", "]", ".", ",", ";");

    public Lexer(Reader input) {
        this.input = input;
        readNextChar();
    }

    private void readNextChar() {
        try {
            currentChar = input.read();
        } catch (IOException e) {
            currentChar = -1;
        }
    }

    public Symbol getNextSymbol() {
        // حذف فضاهای خالی
        while (Character.isWhitespace(currentChar)) {
            readNextChar();
        }

        if (currentChar == -1) {
            return new Symbol<String>("EOF", "");
        }

        // حذف کامنت‌ها
        if (currentChar == '#') {
            while (currentChar != '\n' && currentChar != -1) {
                readNextChar();
            }
            return getNextSymbol();
        }

        // تشخیص اعداد (INT و FLOAT)
        if (Character.isDigit(currentChar)) {
            StringBuilder buffer = new StringBuilder();
            boolean isFloat = false;
            while (Character.isDigit(currentChar) || currentChar == '.') {
                if (currentChar == '.') isFloat = true;
                buffer.append((char) currentChar);
                readNextChar();
            }
            if (isFloat) return new Symbol<Double>("FLOAT", Double.parseDouble(buffer.toString()));
            return new Symbol<Integer>("INT", Integer.parseInt(buffer.toString()));
        }

        // تشخیص کلمات (IDENTIFIER, KEYWORD, TYPE, BOOLEAN)
        if (Character.isLetter(currentChar) || currentChar == '_') {
            StringBuilder buffer = new StringBuilder();
            while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
                buffer.append((char) currentChar);
                readNextChar();
            }
            String result = buffer.toString();

            if (KEYWORDS.contains(result)) return new Symbol<String>("KEYWORD", result);
            if (TYPES.contains(result)) return new Symbol<String>("TYPE", result);
            if (result.equals("true") || result.equals("false")) return new Symbol<Boolean>("BOOLEAN", Boolean.parseBoolean(result));

            // نکته مهم: برای هماهنگی با پارسر، همه اسم‌ها را IDENTIFIER می‌نامیم
            return new Symbol<String>("IDENTIFIER", result);
        }

        // تشخیص رشته‌ها (STRING)
        if (currentChar == '"') {
            StringBuilder buffer = new StringBuilder();
            readNextChar(); // رد کردن " اول
            while (currentChar != '"' && currentChar != -1) {
                if (currentChar == '\\') { // مدیریت escape characters
                    readNextChar();
                    if (currentChar == 'n') buffer.append('\n');
                    else if (currentChar == 't') buffer.append('\t');
                    else buffer.append((char) currentChar);
                } else {
                    buffer.append((char) currentChar);
                }
                readNextChar();
            }
            readNextChar(); // رد کردن " دوم
            return new Symbol<String>("STRING", buffer.toString());
        }

        // تشخیص عملگرها و کاراکترهای خاص
        String singleChar = String.valueOf((char) currentChar);

        // چک کردن عملگرهای چند کاراکتری مثل == یا -> یا =/=
        if (currentChar == '=' || currentChar == '!' || currentChar == '-' || currentChar == '>' || currentChar == '<' || currentChar == '&' || currentChar == '|') {
            StringBuilder opBuffer = new StringBuilder();
            opBuffer.append((char) currentChar);

            try {
                input.mark(2);
                int next = input.read();
                String doubleOp = opBuffer.toString() + (char) next;

                // چک کردن برای =/=
                if (doubleOp.equals("=/")) {
                    int third = input.read();
                    if (third == '=') {
                        readNextChar(); readNextChar(); // چون currentChar الان روی کاراکتر اول است
                        readNextChar();
                        return new Symbol<String>("OPERATOR", "=/=");
                    }
                    input.reset();
                } else if (OPERATORS.contains(doubleOp)) {
                    readNextChar(); readNextChar();
                    return new Symbol<String>("OPERATOR", doubleOp);
                } else {
                    input.reset();
                }
            } catch (IOException e) {}
        }

        if (OPERATORS.contains(singleChar)) {
            readNextChar();
            return new Symbol<String>("OPERATOR", singleChar);
        }

        if (SPECIAL_CHARACTERS.contains(singleChar)) {
            readNextChar();
            return new Symbol<String>("SPECIAL_CHARACTER", singleChar);
        }

        throw new RuntimeException("Lexical Error: Unrecognized character '" + (char)currentChar + "'");
    }
}