package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Parser.Parser;
import compiler.Parser.ASTNode;

public class Compiler {
    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("-lexer")) {
                String filePath = args[1];
                try (java.io.FileReader reader = new java.io.FileReader(filePath)) {
                    Lexer lexer = new Lexer(reader);
                    while (true) {
                        Symbol symbol = lexer.getNextSymbol();
                        if (symbol.toString().equals("<EOF, >") || symbol.getType().equals("EOF")) {
                            System.out.println("End of file reached.");
                            break;
                        }
                        System.out.println(symbol);
                    }
                } catch (java.io.IOException e) {
                    System.out.println("Error reading file: " + e.getMessage());
                }
            }
            //
            else if (args[0].equals("-parser")) {
                String filePath = args[1];
                try (java.io.FileReader reader = new java.io.FileReader(filePath)) {
                    Lexer lexer = new Lexer(reader);
                    Parser parser = new Parser(lexer);
                    ASTNode root = parser.getAST();

                    if (root != null) {
                        root.print(""); //
                        compiler.Semantic.SemanticAnalyzer analyzer = new compiler.Semantic.SemanticAnalyzer();
                        analyzer.analyze(root);
                        // --------------------------------

                    }
                } catch (java.io.IOException e) {
                    System.out.println("Error reading file: " + e.getMessage());
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                }
            }
            else {
                System.out.println("Unknown option: " + args[0]);
            }
        } else {
            System.out.println("No option provided. Use -lexer <file_path> or -parser <file_path>.");
        }
    }
}