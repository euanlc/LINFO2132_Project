package compiler.Semantic;

import compiler.Parser.*;
import java.util.*;

public class SemanticAnalyzer {
    private Stack<Map<String, String>> symbolTables;

    // برای ذخیره توابع و کالکشن‌ها جهت بررسی ارورهای ArgumentError و CollectionError
    private Map<String, List<String>> functionSignatures;
    private Map<String, String> collectionTypes;
    private String currentFunctionReturnType = null;

    public SemanticAnalyzer() {
        symbolTables = new Stack<>();
        symbolTables.push(new HashMap<>());
        functionSignatures = new HashMap<>();
        collectionTypes = new HashMap<>();
    }

    public void analyze(ASTNode root) {
        visit(root);
        System.out.println("Semantic Analysis Passed Successfully!");
    }

    private void throwError(String errorType, String message) {
        System.err.println(errorType + ": " + message);
        System.exit(2);
    }

    private String visit(ASTNode node) {
        if (node == null) return "void";

        if (node instanceof BlockNode) {
            symbolTables.push(new HashMap<>());
            for (ASTNode stmt : ((BlockNode) node).statements) {
                visit(stmt);
            }
            symbolTables.pop();
            return "void";
        }

        // 1. TypeError
        if (node instanceof VarDeclNode) {
            VarDeclNode varNode = (VarDeclNode) node;
            symbolTables.peek().put(varNode.varName, varNode.varType);

            if (varNode.expression != null) {
                String exprType = visit(varNode.expression);
                if (!exprType.equals("UNKNOWN") && !varNode.varType.equals(exprType)) {
                    if (!(varNode.varType.equals("FLOAT") && exprType.equals("INT"))) {
                        throwError("TypeError", "Cannot assign " + exprType + " to " + varNode.varType);
                    }
                }
            }
            return "void";
        }

        // 2. ScopeError
        if (node instanceof VariableNode) {
            String varName = ((VariableNode) node).name;
            if (!isVariableDeclared(varName)) {
                throwError("ScopeError", "Variable '" + varName + "' is used out of scope or not defined.");
            }
            return getVariableType(varName);
        }

        // 3. OperatorError
        if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            String leftType = visit(binOp.left);
            String rightType = visit(binOp.right);

            if (!leftType.equals("UNKNOWN") && !rightType.equals("UNKNOWN")) {
                if (!leftType.equals(rightType) && !(leftType.equals("FLOAT") && rightType.equals("INT")) && !(leftType.equals("INT") && rightType.equals("FLOAT"))) {
                    throwError("OperatorError", "Cannot apply operator to " + leftType + " and " + rightType);
                }
            }
            if (binOp.operator.matches("==|=/=|>|<|>=|<=")) return "BOOL";
            if (leftType.equals("FLOAT") || rightType.equals("FLOAT")) return "FLOAT";
            return "INT";
        }

        // 4. MissingConditionError (If)
        if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            String conditionType = visit(ifNode.condition);
            if (!conditionType.equals("BOOL") && !conditionType.equals("UNKNOWN")) {
                throwError("MissingConditionError", "Condition in 'if' statement must be a BOOL.");
            }
            visit(ifNode.thenBlock);
            if (ifNode.elseBlock != null) visit(ifNode.elseBlock);
            return "void";
        }

        // 5. MissingConditionError (While)
        if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            String conditionType = visit(whileNode.condition);
            if (!conditionType.equals("BOOL") && !conditionType.equals("UNKNOWN")) {
                throwError("MissingConditionError", "Condition in 'while' statement must be a BOOL.");
            }
            visit(whileNode.bodyStmt);
            return "void";
        }

        // 6. ReturnError و مدیریت توابع
        if (node instanceof FunctionNode) {
            FunctionNode funcNode = (FunctionNode) node;
            List<String> paramTypes = new ArrayList<>();
            symbolTables.push(new HashMap<>());

            for (ASTNode param : funcNode.parameters) {
                VarDeclNode p = (VarDeclNode) param;
                paramTypes.add(p.varType);
                symbolTables.peek().put(p.varName, p.varType);
            }
            functionSignatures.put(funcNode.name, paramTypes); // ذخیره امضای تابع

            currentFunctionReturnType = funcNode.returnType;
            visit(funcNode.body);
            currentFunctionReturnType = null;
            symbolTables.pop();
            return "void";
        }

        // هندل کردن خود عبارت return
        if (node instanceof ReturnNode) {
            ReturnNode retNode = (ReturnNode) node;
            String actualType = "void";
            if (retNode.expression != null) {
                actualType = visit(retNode.expression);
            }
            if (currentFunctionReturnType != null && !currentFunctionReturnType.equals("void") && !currentFunctionReturnType.equals(actualType)) {
                if (!(currentFunctionReturnType.equals("FLOAT") && actualType.equals("INT"))) {
                    throwError("ReturnError", "Function expected " + currentFunctionReturnType + " but returned " + actualType);
                }
            }
            return "void";
        }

        // 7. ArgumentError
        if (node instanceof FunctionCallNode) {
            FunctionCallNode callNode = (FunctionCallNode) node;
            if (functionSignatures.containsKey(callNode.functionName)) {
                List<String> expectedTypes = functionSignatures.get(callNode.functionName);
                if (expectedTypes.size() != callNode.arguments.size()) {
                    throwError("ArgumentError", "Argument count mismatch in function " + callNode.functionName);
                }
                for (int i = 0; i < expectedTypes.size(); i++) {
                    String actualArgType = visit(callNode.arguments.get(i));
                    String expectedArgType = expectedTypes.get(i);
                    if (!expectedArgType.equals(actualArgType) && !(expectedArgType.equals("FLOAT") && actualArgType.equals("INT"))) {
                        throwError("ArgumentError", "Argument type mismatch in function " + callNode.functionName);
                    }
                }
            }
            return "UNKNOWN";
        }

        // 8. CollectionError
        if (node instanceof CollectionNode) {
            CollectionNode collNode = (CollectionNode) node;
            if (!Character.isUpperCase(collNode.name.charAt(0))) {
                throwError("CollectionError", "Collection name must start with a capital letter.");
            }
            if (collectionTypes.containsKey(collNode.name)) {
                throwError("CollectionError", "Collection " + collNode.name + " is already defined.");
            }
            collectionTypes.put(collNode.name, "COLL");
            return "void";
        }

        if (node instanceof IntegerNode) return "INT";
        if (node instanceof FloatNode) return "FLOAT";
        if (node instanceof StringNode) return "STRING";
        if (node instanceof BooleanNode) return "BOOL";

        return "UNKNOWN";
    }

    private boolean isVariableDeclared(String name) {
        for (int i = symbolTables.size() - 1; i >= 0; i--) {
            if (symbolTables.get(i).containsKey(name)) return true;
        }
        return false;
    }

    private String getVariableType(String name) {
        for (int i = symbolTables.size() - 1; i >= 0; i--) {
            if (symbolTables.get(i).containsKey(name)) return symbolTables.get(i).get(name);
        }
        return "UNKNOWN";
    }
}