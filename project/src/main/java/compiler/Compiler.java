package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Parser.Parser;
import compiler.Parser.ASTNode;
import compiler.Semantic.SemanticAnalyzer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Compiler {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No file provided.");
            return;
        }

        String sourceFile = "";
        String targetPath = "Main.class"; // مسیر پیش‌فرض اگر -o داده نشد
        boolean runLexerOnly = false;

        // پردازش آرگومان‌های ورودی
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-lexer")) {
                runLexerOnly = true;
                if (i + 1 < args.length) sourceFile = args[i + 1];
            } else if (args[i].equals("-parser")) {
                if (i + 1 < args.length) sourceFile = args[i + 1];
            } else if (args[i].equals("-o")) {
                if (i + 1 < args.length) {
                    targetPath = args[i + 1];
                    i++; // پرش از روی اسم فایل تارگت
                }
            } else if (sourceFile.isEmpty() && !args[i].startsWith("-")) {
                sourceFile = args[i];
            }
        }

        if (sourceFile.isEmpty()) {
            System.err.println("No source file specified.");
            return;
        }

        if (runLexerOnly) {
            try (java.io.FileReader reader = new java.io.FileReader(sourceFile)) {
                Lexer lexer = new Lexer(reader);
                while (true) {
                    Symbol symbol = lexer.getNextSymbol();
                    if (symbol.toString().equals("<EOF, >") || symbol.getType().equals("EOF")) break;
                    System.out.println(symbol);
                }
            } catch (Exception e) {
                System.err.println("Error reading file: " + e.getMessage());
            }
        } else {
            // اجرای کامل کامپایلر: Lexer -> Parser -> Semantic -> CodeGen
            try (java.io.FileReader reader = new java.io.FileReader(sourceFile)) {
                Lexer lexer = new Lexer(reader);
                Parser parser = new Parser(lexer);
                ASTNode root = parser.getAST();

                if (root != null) {
                    // فاز سمانتیک
                    SemanticAnalyzer analyzer = new SemanticAnalyzer();
                    analyzer.analyze(root);

                    // استخراج مسیر دایرکتوری از targetPath برای ذخیره فایل‌ها
                    Path path = Paths.get(targetPath);
                    String outputDirectory = path.getParent() != null ? path.getParent().toString() : ".";
                    String mainClassName = path.getFileName().toString().replace(".class", "");

                    // ایجاد پوشه خروجی اگر وجود نداشت
                    File dir = new File(outputDirectory);
                    if (!dir.exists()) dir.mkdirs();

                    // فاز تولید کد
                    compiler.CodeGen.CodeGenerator codeGen = new compiler.CodeGen.CodeGenerator(outputDirectory, mainClassName);
                    codeGen.generate(root);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.matches(".*(Error).*")) {
                    System.err.println(msg);
                    System.exit(2);
                }
                System.exit(0);
            }
        }
    }
}