package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private Lexer lexer;
    private Symbol currentSymbol;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentSymbol = lexer.getNextSymbol();
    }

    private void eat(String expectedType) {
        if (currentSymbol.getType().equals(expectedType)) {
            currentSymbol = lexer.getNextSymbol();
        } else {
            throw new RuntimeException("Syntax Error: Expected " + expectedType + " but found " + currentSymbol.getType());
        }
    }

    private void eatValue(String expectedType, String expectedValue) {
        if (currentSymbol.getType().equals(expectedType) && currentSymbol.getValue().equals(expectedValue)) {
            currentSymbol = lexer.getNextSymbol();
        } else {
            throw new RuntimeException("Syntax Error: Expected '" + expectedValue + "' but found '" + currentSymbol.getValue() + "'");
        }
    }

    public ASTNode getAST() {
        List<ASTNode> statements = new ArrayList<>();
        while (!currentSymbol.getType().equals("EOF")) {
            statements.add(parseStatement());
        }
        return new BlockNode(statements);
    }

    private ASTNode parseStatement() {
        // حلال مشکل کلمه final
        if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("final")) {
            eatValue("KEYWORD", "final");
        }

        if (currentSymbol.getType().equals("TYPE")) {
            return parseDeclaration();
        } else if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("if")) {
            return parseIfStatement();
        } else if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("while")) {
            return parseWhileStatement();
        } else if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("for")) {
            return parseForStatement();
        } else if (currentSymbol.getType().equals("SPECIAL_CHARACTER") && currentSymbol.getValue().equals("{")) {
            return parseBlock();
        } else {
            ASTNode expr = parseExpression();
            eatValue("SPECIAL_CHARACTER", ";");
            return expr;
        }
    }

    private ASTNode parseBlock() {
        eatValue("SPECIAL_CHARACTER", "{");
        List<ASTNode> statements = new ArrayList<>();
        
        while (!(currentSymbol.getType().equals("SPECIAL_CHARACTER") && currentSymbol.getValue().equals("}"))) {
            if (currentSymbol.getType().equals("EOF")) {
                throw new RuntimeException("Syntax Error: Unclosed block, expected '}'");
            }
            statements.add(parseStatement());
        }
        eatValue("SPECIAL_CHARACTER", "}");
        return new BlockNode(statements);
    }

    private ASTNode parseIfStatement() {
        eatValue("KEYWORD", "if");
        eatValue("SPECIAL_CHARACTER", "(");
        ASTNode condition = parseExpression();
        eatValue("SPECIAL_CHARACTER", ")");

        ASTNode thenStmt = parseBlock();
        BlockNode thenBlock = (BlockNode) thenStmt;
        BlockNode elseBlock = null;

        if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("else")) {
            eatValue("KEYWORD", "else");
            ASTNode elseStmt = parseBlock();
            elseBlock = (BlockNode) elseStmt;
        }

        return new IfNode(condition, thenBlock, elseBlock);
    }

    private ASTNode parseWhileStatement() {
        eatValue("KEYWORD", "while");
        eatValue("SPECIAL_CHARACTER", "(");
        ASTNode condition = parseExpression();
        eatValue("SPECIAL_CHARACTER", ")");

        ASTNode bodyStmt = parseBlock();
        
        return new WhileNode(condition, (BlockNode) bodyStmt);
    }

    private ASTNode parseForStatement() {
        eatValue("KEYWORD", "for");
        eatValue("SPECIAL_CHARACTER", "(");
        
        String varName = (String) currentSymbol.getValue();
        eat("IDENTIFIER");
        eatValue("SPECIAL_CHARACTER", ";");
        
        ASTNode startExpr = parseTerm(); 
        
        eatValue("OPERATOR", "-");
        eatValue("OPERATOR", ">");
        
        ASTNode endExpr = parseExpression();
        eatValue("SPECIAL_CHARACTER", ";");
        
        ASTNode stepExpr = parseExpression();
        eatValue("SPECIAL_CHARACTER", ")");
        
        ASTNode bodyStmt = parseBlock();
        
        return new ForNode(varName, startExpr, endExpr, stepExpr, (BlockNode) bodyStmt);
    }

    private ASTNode parseDeclaration() {
        String varType = (String) currentSymbol.getValue();
        eat("TYPE");

        String varName = (String) currentSymbol.getValue();
        eat("IDENTIFIER");

        ASTNode expression = null;
        if (currentSymbol.getType().equals("OPERATOR") && currentSymbol.getValue().equals("=")) {
            eat("OPERATOR");
            expression = parseExpression();
        }

        eatValue("SPECIAL_CHARACTER", ";");

        return new VarDeclNode(varType, varName, expression);
    }

    private ASTNode parseExpression() {
        ASTNode left = parseTerm();
        
        while (currentSymbol.getValue().equals("+") || currentSymbol.getValue().equals("-") ||
               currentSymbol.getValue().equals("==") || currentSymbol.getValue().equals("=/=") ||
               currentSymbol.getValue().equals(">") || currentSymbol.getValue().equals("<") ||
               currentSymbol.getValue().equals(">=") || currentSymbol.getValue().equals("<=")) {
            String op = (String) currentSymbol.getValue();
            eat("OPERATOR");
            ASTNode right = parseTerm();
            left = new BinOpNode(op, left, right);
        }
        return left;
    }

    private ASTNode parseTerm() {
        ASTNode left = parseFactor();
        
        while (currentSymbol.getValue().equals("*") || currentSymbol.getValue().equals("/")) {
            String op = (String) currentSymbol.getValue();
            eat("OPERATOR");
            ASTNode right = parseFactor();
            left = new BinOpNode(op, left, right);
        }
        return left;
    }

    private ASTNode parseFactor() {
        if (currentSymbol.getType().equals("INT")) {
            Object val = currentSymbol.getValue();
            int value = (val instanceof Integer) ? (int)val : ((Double)val).intValue();
            eat("INT");
            return new IntegerNode(value);
        } else if (currentSymbol.getType().equals("FLOAT")) {
            Object val = currentSymbol.getValue();
            // چون IntegerNode فقط int می‌گیره، فعلاً کست می‌کنیم به int
            int value = (val instanceof Double) ? ((Double)val).intValue() : (int)val;
            eat("FLOAT");
            return new IntegerNode(value);
        } else if (currentSymbol.getType().equals("IDENTIFIER")) {
            String name = (String) currentSymbol.getValue();
            eat("IDENTIFIER");
            return new VariableNode(name);
        }
        throw new RuntimeException("Syntax Error: Unexpected symbol '" + currentSymbol.getValue() + "'");
    }
}
