package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Parser.Parser;
import compiler.Parser.ASTNode;

public class Compiler {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No file provided.");
            return;
        }

        String filePath = "";
        boolean runLexerOnly = false;

        // بررسی ورودی‌های ترمینال
        if (args.length == 1) {
            // حالت Inginious: فقط اسم فایل رو می‌ده (مثل test.lang)
            filePath = args[0];
        } else if (args.length >= 2) {
            // حالت دستی خودمون: که -lexer یا -parser رو مشخص می‌کنیم
            if (args[0].equals("-lexer")) {
                runLexerOnly = true;
                filePath = args[1];
            } else if (args[0].equals("-parser")) {
                filePath = args[1];
            } else {
                filePath = args[0];
            }
        }

        if (runLexerOnly) {
            // فقط اجرای Lexer
            try (java.io.FileReader reader = new java.io.FileReader(filePath)) {
                Lexer lexer = new Lexer(reader);
                while (true) {
                    Symbol symbol = lexer.getNextSymbol();
                    if (symbol.toString().equals("<EOF, >") || symbol.getType().equals("EOF")) {
                        break;
                    }
                    System.out.println(symbol);
                }
            } catch (java.io.IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            }
        } else {
            // اجرای کامل کامپایلر (Lexer -> Parser -> SemanticAnalyzer)
            try (java.io.FileReader reader = new java.io.FileReader(filePath)) {
                Lexer lexer = new Lexer(reader);
                Parser parser = new Parser(lexer);
                ASTNode root = parser.getAST();

                if (root != null) {
                    // ریشه درخت رو به تحلیل‌گر معنایی می‌دیم
                    compiler.Semantic.SemanticAnalyzer analyzer = new compiler.Semantic.SemanticAnalyzer();
                    analyzer.analyze(root);
                }
            } catch (java.io.IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}