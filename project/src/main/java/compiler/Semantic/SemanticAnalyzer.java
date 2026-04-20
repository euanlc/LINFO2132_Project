package compiler.Semantic;

import compiler.Parser.*;
import java.util.*;

public class SemanticAnalyzer {
    private Stack<Map<String, String>> symbolTables;
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
        registerAllFunctions(root);
        visit(root);
        System.out.println("Semantic Analysis Passed Successfully!");
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
        } else if (node instanceof BlockNode) {
            if (((BlockNode) node).statements != null) {
                for (ASTNode stmt : ((BlockNode) node).statements) registerAllFunctions(stmt);
            }
        }
    }

    private void throwError(String errorType, String message) {
        System.err.println(errorType + ": " + message);
        System.exit(2);
    }

    private String visit(ASTNode node) {
        if (node == null) return "void";

        if (node instanceof BlockNode) {
            symbolTables.push(new HashMap<>());
            if (((BlockNode) node).statements != null) {
                for (ASTNode stmt : ((BlockNode) node).statements) {
                    visit(stmt);
                }
            }
            symbolTables.pop();
            return "void";
        }

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

        if (node instanceof VariableNode) {
            String varName = ((VariableNode) node).name;
            if (!isVariableDeclared(varName)) {
                throwError("ScopeError", "Variable '" + varName + "' is used out of scope or not defined.");
            }
            return getVariableType(varName);
        }

        if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            String leftType = visit(binOp.left);
            String rightType = visit(binOp.right);

            if (!leftType.equals("UNKNOWN") && !rightType.equals("UNKNOWN")) {
                if (!leftType.equals(rightType) && !(leftType.equals("FLOAT") && rightType.equals("INT")) && !(leftType.equals("INT") && rightType.equals("FLOAT"))) {
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

        if (node instanceof FunctionCallNode) {
            FunctionCallNode callNode = (FunctionCallNode) node;
            if (functionSignatures.containsKey(callNode.functionName)) {
                List<String> expectedTypes = functionSignatures.get(callNode.functionName);

                int actualSize = (callNode.arguments != null) ? callNode.arguments.size() : 0;

                if (expectedTypes.size() != actualSize) {
                    throwError("ArgumentError", "Argument count mismatch in function " + callNode.functionName);
                }

                if (callNode.arguments != null) {
                    for (int i = 0; i < expectedTypes.size(); i++) {
                        String actualArgType = visit(callNode.arguments.get(i));
                        String expectedArgType = expectedTypes.get(i);
                        if (!expectedArgType.equals(actualArgType) && !(expectedArgType.equals("FLOAT") && actualArgType.equals("INT"))) {
                            throwError("ArgumentError", "Argument type mismatch in function " + callNode.functionName);
                        }
                    }
                }
            }
            return "UNKNOWN";
        }

        if (node instanceof CollectionNode) {
            CollectionNode collNode = (CollectionNode) node;
            if (collNode.name != null && !collNode.name.isEmpty()) {
                if (!Character.isUpperCase(collNode.name.charAt(0))) {
                    throwError("CollectionError", "Collection name must start with a capital letter.");
                }
                if (collectionTypes.containsKey(collNode.name)) {
                    throwError("CollectionError", "Collection " + collNode.name + " is already defined.");
                }
                collectionTypes.put(collNode.name, "COLL");
            }
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