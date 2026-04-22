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
        if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("final")) {
            eatValue("KEYWORD", "final");
        }

        // --- بخش جدید: تشخیص توابع، کالکشن‌ها و خروجی‌ها ---
        if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("def")) {
            return parseFunction();
        } else if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("coll")) {
            return parseCollection();
        } else if (currentSymbol.getType().equals("KEYWORD") && currentSymbol.getValue().equals("return")) {
            return parseReturn();
        }
        // --------------------------------------------------
        else if (currentSymbol.getType().equals("TYPE") ||
                (currentSymbol.getType().equals("IDENTIFIER") && isCustomType())) { // مدیریت نوع داده کاستوم (کالکشن)
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

    // یک متد کمکی برای اینکه بفهمیم IDENTIFIER در واقع اسم یک کالکشن هست یا نه
    private boolean isCustomType() {
        String val = (String) currentSymbol.getValue();
        return Character.isUpperCase(val.charAt(0));
    }

    private ASTNode parseFunction() {
        eatValue("KEYWORD", "def");
        String returnType = "void";
        if (currentSymbol.getType().equals("TYPE")) {
            returnType = (String) currentSymbol.getValue();
            eat("TYPE");
        }

        String name = (String) currentSymbol.getValue();
        eat("IDENTIFIER");

        eatValue("SPECIAL_CHARACTER", "(");
        List<ASTNode> parameters = new ArrayList<>();
        if (!currentSymbol.getValue().equals(")")) {
            parameters.add(parseDeclarationWithoutSemicolon());
            while (currentSymbol.getType().equals("SPECIAL_CHARACTER") && currentSymbol.getValue().equals(",")) {
                eatValue("SPECIAL_CHARACTER", ",");
                parameters.add(parseDeclarationWithoutSemicolon());
            }
        }
        eatValue("SPECIAL_CHARACTER", ")");

        ASTNode body = parseBlock();
        return new FunctionNode(returnType, name, parameters, (BlockNode) body);
    }

    private ASTNode parseCollection() {
        eatValue("KEYWORD", "coll");
        String name = String.valueOf(currentSymbol.getValue());

        // مچ‌گیری تله‌ی اسم‌های غیرمجاز
        if (currentSymbol.getType().equals("TYPE") || currentSymbol.getType().equals("KEYWORD") || !Character.isUpperCase(name.charAt(0))) {
            System.out.println("CollectionError: Invalid name");
            System.err.println("CollectionError: Invalid name");
            System.exit(2);
        }

        currentSymbol = lexer.getNextSymbol(); // عبور امن
        eatValue("SPECIAL_CHARACTER", "{");
        List<ASTNode> fields = new ArrayList<>();
        while (!currentSymbol.getValue().equals("}")) {
            fields.add(parseDeclaration());
        }
        eatValue("SPECIAL_CHARACTER", "}");
        return new CollectionNode(name, fields);
    }
    private ASTNode parseReturn() {
        eatValue("KEYWORD", "return");
        ASTNode expr = parseExpression();
        eatValue("SPECIAL_CHARACTER", ";");
        return new ReturnNode(expr);
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
        ASTNode decl = parseDeclarationWithoutSemicolon();
        eatValue("SPECIAL_CHARACTER", ";");
        return decl;
    }

    // متد کمکی برای گرفتن پارامترهای تابع که سمی‌کالن آخرش ندارن
    private ASTNode parseDeclarationWithoutSemicolon() {
        String varType = (String) currentSymbol.getValue();
        if (currentSymbol.getType().equals("TYPE") || currentSymbol.getType().equals("IDENTIFIER")) {
            currentSymbol = lexer.getNextSymbol();
        } else {
            throw new RuntimeException("Syntax Error: Expected TYPE");
        }

        if (currentSymbol.getType().equals("SPECIAL_CHARACTER") && currentSymbol.getValue().equals("[")) {
            eatValue("SPECIAL_CHARACTER", "[");
            eatValue("SPECIAL_CHARACTER", "]");
            varType += "[]";
        }

        String varName = (String) currentSymbol.getValue();
        eat("IDENTIFIER");

        ASTNode expression = null;
        if (currentSymbol.getType().equals("OPERATOR") && currentSymbol.getValue().equals("=")) {
            eat("OPERATOR");
            expression = parseExpression();
        }
        return new VarDeclNode(varType, varName, expression);
    }

    private ASTNode parseExpression() {
        ASTNode left = parseTerm();

        while (currentSymbol.getValue().equals("=") || currentSymbol.getValue().equals("+") || currentSymbol.getValue().equals("-") ||
                currentSymbol.getValue().equals("==") || currentSymbol.getValue().equals("=/=") ||
                currentSymbol.getValue().equals(">") || currentSymbol.getValue().equals("<") ||
                currentSymbol.getValue().equals(">=") || currentSymbol.getValue().equals("<=") ||
                currentSymbol.getValue().equals("&&") || currentSymbol.getValue().equals("||")) {
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
            int value = ((Number) val).intValue();
            eat("INT");
            return new IntegerNode(value);
        } else if (currentSymbol.getType().equals("FLOAT")) {
            Object val = currentSymbol.getValue();
            double value = ((Number) val).doubleValue();
            eat("FLOAT");
            return new FloatNode(value);
        } else if (currentSymbol.getType().equals("STRING")) {
            String val = (String) currentSymbol.getValue();
            eat("STRING");
            return new StringNode(val);
        }
        // --- بخش اصلاح شده برای شناختن راحت‌ترِ true و false ---
        else if (String.valueOf(currentSymbol.getValue()).equals("true") || String.valueOf(currentSymbol.getValue()).equals("false")) {
            boolean val = Boolean.parseBoolean(String.valueOf(currentSymbol.getValue()));
            currentSymbol = lexer.getNextSymbol(); // با موفقیت ازش عبور می‌کنیم
            return new BooleanNode(val);
        }
        // --------------------------------------------------------
        else if (currentSymbol.getType().equals("IDENTIFIER")) {
            String name = (String) currentSymbol.getValue();
            eat("IDENTIFIER");

            // اگر بعد از اسم پرانتز بود، یعنی فراخوانی تابع (مثل square(value))
            if (currentSymbol.getType().equals("SPECIAL_CHARACTER") && currentSymbol.getValue().equals("(")) {
                eatValue("SPECIAL_CHARACTER", "(");
                List<ASTNode> args = new ArrayList<>();
                if (!currentSymbol.getValue().equals(")")) {
                    args.add(parseExpression());
                    while (currentSymbol.getType().equals("SPECIAL_CHARACTER") && currentSymbol.getValue().equals(",")) {
                        eatValue("SPECIAL_CHARACTER", ",");
                        args.add(parseExpression());
                    }
                }
                eatValue("SPECIAL_CHARACTER", ")");
                return new FunctionCallNode(name, args);
            }

            return new VariableNode(name);
        }
        throw new RuntimeException("Syntax Error: Unexpected symbol '" + currentSymbol.getValue() + "'");
    }}