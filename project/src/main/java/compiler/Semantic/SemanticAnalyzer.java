package compiler.Semantic;

import compiler.Parser.*;
import java.util.*;

public class SemanticAnalyzer {
    private Stack<Map<String, String>> symbolTables;
    private Map<String, List<String>> functionSignatures;
    private Map<String, String> functionReturnTypes;
    private Map<String, String> collectionTypes;
    private String currentFunctionReturnType = null;

    public SemanticAnalyzer() {
        symbolTables = new Stack<>();
        symbolTables.push(new HashMap<>());
        functionSignatures = new HashMap<>();
        functionReturnTypes = new HashMap<>();
        collectionTypes = new HashMap<>();
    }

    public void analyze(ASTNode root) {
        registerAllCollections(root);
        registerAllFunctions(root);
        visit(root);
        System.out.println("Semantic Analysis Passed Successfully!");
    }

    private void registerAllCollections(ASTNode node) {
        if (node == null) return;
        String className = node.getClass().getSimpleName().toLowerCase();

        if (className.contains("coll")) {
            try {
                String nameVal = null;
                for (java.lang.reflect.Field field : node.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getType() == String.class) {
                        String val = (String) field.get(node);
                        if (val != null && !val.isEmpty()) {
                            nameVal = val;
                            break;
                        }
                    }
                }
                if (nameVal != null) {
                    if (!Character.isUpperCase(nameVal.charAt(0)))
                        throwError("CollectionError", "Capital letter required");
                    if (collectionTypes.containsKey(nameVal)) throwError("CollectionError", "Already defined");
                    collectionTypes.put(nameVal, "COLL");
                }
            } catch (Exception e) {}
        }

        try {
            for (java.lang.reflect.Field field : node.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object obj = field.get(node);
                if (obj instanceof ASTNode) registerAllCollections((ASTNode) obj);
                else if (obj instanceof Iterable) {
                    for (Object item : (Iterable<?>) obj) {
                        if (item instanceof ASTNode) registerAllCollections((ASTNode) item);
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void registerAllFunctions(ASTNode node) {
        if (node == null) return;

        if (node instanceof FunctionNode) {
            FunctionNode funcNode = (FunctionNode) node;
            List<String> paramTypes = new ArrayList<>();
            if (funcNode.parameters != null) {
                for (ASTNode param : funcNode.parameters) {
                    if (param instanceof VarDeclNode) {
                        paramTypes.add(((VarDeclNode) param).varType);
                    }
                }
            }
            functionSignatures.put(funcNode.name, paramTypes);
            functionReturnTypes.put(funcNode.name, funcNode.returnType);
        }

        try {
            for (java.lang.reflect.Field field : node.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object obj = field.get(node);
                if (obj instanceof ASTNode) {
                    registerAllFunctions((ASTNode) obj);
                } else if (obj instanceof Iterable) {
                    for (Object item : (Iterable<?>) obj) {
                        if (item instanceof ASTNode) registerAllFunctions((ASTNode) item);
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void throwError(String errorType, String message) {
        System.out.println(errorType + ": " + message);
        System.err.println(errorType + ": " + message);
        System.exit(2);
    }

    private String visit(ASTNode node) {
        if (node == null) return "void";

        if (node instanceof BlockNode) {
            symbolTables.push(new HashMap<>());
            if (((BlockNode) node).statements != null) {
                for (ASTNode stmt : ((BlockNode) node).statements) visit(stmt);
            }
            symbolTables.pop();
            return "void";
        }

        if (node instanceof VarDeclNode) {
            VarDeclNode varNode = (VarDeclNode) node;
            String t = varNode.varType;
            if (t != null && !t.equals("INT") && !t.equals("FLOAT") && !t.equals("STRING") && !t.equals("BOOL") && !t.equals("UNKNOWN") && !t.equals("void")) {
                if (!collectionTypes.containsKey(t)) throwError("CollectionError", "Undefined collection type: " + t);
            }

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

        if (node instanceof VariableNode) {
            String varName = ((VariableNode) node).name;
            if (!isVariableDeclared(varName)) {
                throwError("ScopeError", "Variable '" + varName + "' is used out of scope.");
            }
            return getVariableType(varName);
        }

        if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            String leftType = visit(binOp.left);
            String rightType = visit(binOp.right);

            if (!leftType.equals("UNKNOWN") && !rightType.equals("UNKNOWN")) {
                if (!leftType.equals(rightType) && !(leftType.equals("FLOAT") && rightType.equals("INT")) && !(leftType.equals("INT") && rightType.equals("FLOAT"))) {
                    // تغییر مهم ۱: نام ارور به OperatorError تغییر یافت
                    throwError("OperatorError", "Cannot apply operator to " + leftType + " and " + rightType);
                }
            }
            if (binOp.operator.matches("==|!=|=/=|>|<|>=|<=|&&|\\|\\||and|or")) return "BOOL";
            if (leftType.equals("STRING") || rightType.equals("STRING")) return "STRING";
            if (leftType.equals("FLOAT") || rightType.equals("FLOAT")) return "FLOAT";
            return "INT";
        }

        if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            String conditionType = visit(ifNode.condition);
            if (!conditionType.equals("BOOL") && !conditionType.equals("UNKNOWN")) {
                // تغییر مهم ۲: نام ارور به MissingConditionError تغییر یافت
                throwError("MissingConditionError", "Condition in 'if' statement must be a BOOL.");
            }
            if (ifNode.thenBlock != null) visit(ifNode.thenBlock);
            if (ifNode.elseBlock != null) visit(ifNode.elseBlock);
            return "void";
        }

        if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            String conditionType = visit(whileNode.condition);
            if (!conditionType.equals("BOOL") && !conditionType.equals("UNKNOWN")) {
                // تغییر مهم ۳: نام ارور به MissingConditionError تغییر یافت
                throwError("MissingConditionError", "Condition in 'while' statement must be a BOOL.");
            }
            if (whileNode.bodyStmt != null) visit(whileNode.bodyStmt);
            return "void";
        }

        if (node instanceof FunctionNode) {
            FunctionNode funcNode = (FunctionNode) node;
            symbolTables.push(new HashMap<>());
            if (funcNode.parameters != null) {
                for (ASTNode param : funcNode.parameters) {
                    if (param instanceof VarDeclNode) {
                        VarDeclNode p = (VarDeclNode) param;
                        symbolTables.peek().put(p.varName, p.varType);
                    }
                }
            }
            currentFunctionReturnType = funcNode.returnType;
            if (funcNode.body != null) visit(funcNode.body);
            currentFunctionReturnType = null;
            symbolTables.pop();
            return "void";
        }

        if (node instanceof ReturnNode) {
            ReturnNode retNode = (ReturnNode) node;
            String actualType = "void";
            if (retNode.expression != null) actualType = visit(retNode.expression);
            if (currentFunctionReturnType != null && !currentFunctionReturnType.equals("void") && !currentFunctionReturnType.equals(actualType)) {
                if (!(currentFunctionReturnType.equals("FLOAT") && actualType.equals("INT"))) {
                    // تغییر مهم ۴: نام ارور به ReturnError تغییر یافت
                    throwError("ReturnError", "Function expected " + currentFunctionReturnType + " but returned " + actualType);
                }
            }
            return "void";
        }

        if (node instanceof FunctionCallNode) {
            FunctionCallNode callNode = (FunctionCallNode) node;
            if (functionSignatures.containsKey(callNode.functionName)) {
                List<String> expectedTypes = functionSignatures.get(callNode.functionName);
                int actualSize = (callNode.arguments != null) ? callNode.arguments.size() : 0;

                if (expectedTypes.size() != actualSize) {
                    // تغییر مهم ۵: نام ارور به ArgumentError تغییر یافت
                    throwError("ArgumentError", "Argument count mismatch");
                }

                if (callNode.arguments != null) {
                    for (int i = 0; i < expectedTypes.size(); i++) {
                        String actualArgType = visit(callNode.arguments.get(i));
                        String expectedArgType = expectedTypes.get(i);
                        if (!expectedArgType.equals(actualArgType) && !(expectedArgType.equals("FLOAT") && actualArgType.equals("INT"))) {
                            // تغییر مهم ۶: نام ارور به ArgumentError تغییر یافت
                            throwError("ArgumentError", "Argument type mismatch");
                        }
                    }
                }
                return functionReturnTypes.getOrDefault(callNode.functionName, "UNKNOWN");
            }
            return "UNKNOWN";
        }

        if (node instanceof IntegerNode) return "INT";
        if (node instanceof FloatNode) return "FLOAT";
        if (node instanceof StringNode) return "STRING";
        if (node instanceof BooleanNode) return "BOOL";

        String className = node.getClass().getSimpleName().toLowerCase();

        if (className.contains("vardecl") || className.contains("decl")) {
            try {
                String t = null;
                for (java.lang.reflect.Field field : node.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getName().toLowerCase().contains("type") && field.getType() == String.class) {
                        t = (String) field.get(node);
                    }
                }
                if (t != null && !t.equals("INT") && !t.equals("FLOAT") && !t.equals("STRING") && !t.equals("BOOL") && !t.equals("UNKNOWN") && !t.equals("void")) {
                    if (!collectionTypes.containsKey(t)) throwError("CollectionError", "Undefined collection");
                }
            } catch (Exception e) {}
        }

        boolean isAssignment = className.contains("assign");
        String assignedVarName = null;
        ASTNode assignedExpr = null;

        try {
            for (java.lang.reflect.Field field : node.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object obj = field.get(node);

                if (isAssignment) {
                    if (obj instanceof String) assignedVarName = (String) obj;
                    if (obj instanceof VariableNode) assignedVarName = ((VariableNode) obj).name;
                    if (obj instanceof ASTNode && !(obj instanceof VariableNode)) assignedExpr = (ASTNode) obj;
                }

                if (obj instanceof ASTNode) visit((ASTNode) obj);
                else if (obj instanceof Iterable) {
                    for (Object item : (Iterable<?>) obj) {
                        if (item instanceof ASTNode) visit((ASTNode) item);
                    }
                }
            }

            if (isAssignment && assignedVarName != null && assignedExpr != null) {
                String exprType = visit(assignedExpr);
                String varType = getVariableType(assignedVarName);
                if (!varType.equals("UNKNOWN") && !exprType.equals("UNKNOWN") && !varType.equals(exprType)) {
                    if (!(varType.equals("FLOAT") && exprType.equals("INT"))) {
                        throwError("TypeError", "Cannot assign " + exprType + " to " + varType);
                    }
                }
            }
        } catch (Exception e) {}

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