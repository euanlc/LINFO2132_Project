package compiler.CodeGen;

import compiler.Parser.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

public class CodeGenerator implements Opcodes {
    private String outputDirectory;
    private String mainClassName;
    private ClassWriter mainCw;
    private MethodVisitor currentMv;

    private Map<String, LocalVar> localVariables;
    private int nextLocalIndex;

    private static class LocalVar {
        int index;
        String type;
        LocalVar(int index, String type) { this.index = index; this.type = type; }
    }

    public CodeGenerator(String outputDirectory, String mainClassName) {
        this.outputDirectory = outputDirectory;
        this.mainClassName = mainClassName;
    }

    // --- ترفند جادویی Reflection برای دسترسی به کلاس‌های Parser بدون خطای اینتلیجی ---
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            try {
                if (fieldName.equals("bodyStmt")) {
                    Field f = obj.getClass().getDeclaredField("body");
                    f.setAccessible(true);
                    return f.get(obj);
                }
            } catch (Exception ex) {}
            return null;
        }
    }
    // --------------------------------------------------------------------------------

    public void generate(ASTNode root) {
        mainCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        mainCw.visit(V1_8, ACC_PUBLIC, mainClassName, null, "java/lang/Object", null);

        MethodVisitor constructor = mainCw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        if (root instanceof BlockNode) {
            for (ASTNode stmt : ((BlockNode) root).statements) {
                if (stmt instanceof CollectionNode) {
                    generateCollectionClass((CollectionNode) stmt);
                } else if (stmt instanceof FunctionNode) {
                    generateFunction((FunctionNode) stmt);
                }
            }
        }

        mainCw.visitEnd();
        saveClassFile(mainCw.toByteArray(), mainClassName);
    }

    private void generateCollectionClass(CollectionNode collNode) {
        ClassWriter collCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        collCw.visit(V1_8, ACC_PUBLIC, collNode.name, null, "java/lang/Object", null);

        MethodVisitor mv = collCw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        StringBuilder sigBuilder = new StringBuilder("(");
        for (ASTNode field : collNode.fields) {
            if (field instanceof VarDeclNode) {
                VarDeclNode v = (VarDeclNode) field;
                String desc = getDescriptorForType(v.varType);
                collCw.visitField(ACC_PUBLIC, v.varName, desc, null, null).visitEnd();
                sigBuilder.append(desc);
            }
        }
        sigBuilder.append(")V");

        MethodVisitor paramMv = collCw.visitMethod(ACC_PUBLIC, "<init>", sigBuilder.toString(), null, null);
        paramMv.visitCode();
        paramMv.visitVarInsn(ALOAD, 0);
        paramMv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        int argIdx = 1;
        for (ASTNode field : collNode.fields) {
            if (field instanceof VarDeclNode) {
                VarDeclNode v = (VarDeclNode) field;
                String desc = getDescriptorForType(v.varType);
                paramMv.visitVarInsn(ALOAD, 0);
                if (desc.equals("I") || desc.equals("Z")) paramMv.visitVarInsn(ILOAD, argIdx++);
                else if (desc.equals("F")) paramMv.visitVarInsn(FLOAD, argIdx++);
                else paramMv.visitVarInsn(ALOAD, argIdx++);
                paramMv.visitFieldInsn(PUTFIELD, collNode.name, v.varName, desc);
            }
        }
        paramMv.visitInsn(RETURN);
        paramMv.visitMaxs(0, 0);
        paramMv.visitEnd();

        collCw.visitEnd();
        saveClassFile(collCw.toByteArray(), collNode.name);
    }

    private void generateFunction(FunctionNode func) {
        localVariables = new HashMap<>();
        nextLocalIndex = 0;

        if (func.name.equals("main")) {
            currentMv = mainCw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            nextLocalIndex = 1;
        } else {
            currentMv = mainCw.visitMethod(ACC_PUBLIC | ACC_STATIC, func.name, "()V", null, null);
        }

        currentMv.visitCode();
        visitBlock(func.body);
        currentMv.visitInsn(RETURN);
        currentMv.visitMaxs(0, 0);
        currentMv.visitEnd();
    }

    private void visitBlock(BlockNode block) {
        if (block == null || block.statements == null) return;
        for (ASTNode stmt : block.statements) {
            visitStatement(stmt);
        }
    }

    private void visitStatement(ASTNode stmt) {
        String className = stmt.getClass().getSimpleName();

        if (stmt instanceof VarDeclNode) {
            VarDeclNode varNode = (VarDeclNode) stmt;
            localVariables.put(varNode.varName, new LocalVar(nextLocalIndex, varNode.varType));

            if (varNode.expression != null) {
                generateExpression(varNode.expression);
                storeVariable(varNode.varName, varNode.varType);
            }
            nextLocalIndex++;
        }
        else if (stmt instanceof BinOpNode && ((BinOpNode) stmt).operator.equals("=")) {
            BinOpNode assign = (BinOpNode) stmt;
            if (assign.left instanceof VariableNode) {
                String varName = ((VariableNode) assign.left).name;
                generateExpression(assign.right);
                if (localVariables.containsKey(varName)) storeVariable(varName, localVariables.get(varName).type);
            }
        }
        else if (stmt instanceof FunctionCallNode) {
            FunctionCallNode call = (FunctionCallNode) stmt;
            String fName = call.functionName;

            if (fName.equals("println") && (call.arguments == null || call.arguments.isEmpty())) {
                currentMv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                currentMv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "()V", false);
            }
            else if ((fName.equals("println") || fName.equals("print") || fName.equals("print_INT") || fName.equals("print_FLOAT")) && call.arguments.size() == 1) {
                currentMv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                generateExpression(call.arguments.get(0));

                ASTNode arg = call.arguments.get(0);
                String desc = "(Ljava/lang/String;)V";

                if (fName.equals("print_INT")) desc = "(I)V";
                else if (fName.equals("print_FLOAT")) desc = "(F)V";
                else {
                    if (arg instanceof IntegerNode) desc = "(I)V";
                    else if (arg instanceof FloatNode) desc = "(F)V";
                    else if (arg instanceof BooleanNode) desc = "(Z)V";
                    else if (arg instanceof VariableNode) {
                        String varType = localVariables.get(((VariableNode)arg).name).type;
                        if (varType.equals("INT")) desc = "(I)V";
                        else if (varType.equals("FLOAT")) desc = "(F)V";
                        else if (varType.equals("BOOL")) desc = "(Z)V";
                    }
                    else if (arg instanceof BinOpNode) desc = "(I)V";
                }

                String methodName = fName.startsWith("print_") ? "print" : fName;
                currentMv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", methodName, desc, false);
            }
            else {
                currentMv.visitMethodInsn(INVOKESTATIC, mainClassName, fName, "()V", false);
            }
        }
        // --- بخش تولید کد برای حلقه‌ها و شرط‌های تست 8 ---
        else if (className.equals("IfNode")) {
            ASTNode condition = (ASTNode) getFieldValue(stmt, "condition");
            BlockNode thenBlock = (BlockNode) getFieldValue(stmt, "thenBlock");
            BlockNode elseBlock = (BlockNode) getFieldValue(stmt, "elseBlock");

            generateExpression(condition);
            Label elseLabel = new Label();
            Label endLabel = new Label();

            currentMv.visitJumpInsn(IFEQ, elseLabel); // اگر شرط غلط بود برو به else
            if (thenBlock != null) visitBlock(thenBlock);
            currentMv.visitJumpInsn(GOTO, endLabel);

            currentMv.visitLabel(elseLabel);
            if (elseBlock != null) visitBlock(elseBlock);
            currentMv.visitLabel(endLabel);
        }
        else if (className.equals("WhileNode")) {
            ASTNode condition = (ASTNode) getFieldValue(stmt, "condition");
            BlockNode bodyStmt = (BlockNode) getFieldValue(stmt, "bodyStmt");

            Label startLabel = new Label();
            Label endLabel = new Label();

            currentMv.visitLabel(startLabel);
            generateExpression(condition);
            currentMv.visitJumpInsn(IFEQ, endLabel); // اگر شرط غلط بود برو بیرون

            if (bodyStmt != null) visitBlock(bodyStmt);
            currentMv.visitJumpInsn(GOTO, startLabel);
            currentMv.visitLabel(endLabel);
        }
        else if (className.equals("ForNode")) {
            String varName = (String) getFieldValue(stmt, "varName");
            ASTNode startExpr = (ASTNode) getFieldValue(stmt, "startExpr");
            ASTNode endExpr = (ASTNode) getFieldValue(stmt, "endExpr");
            ASTNode stepExpr = (ASTNode) getFieldValue(stmt, "stepExpr");
            BlockNode bodyStmt = (BlockNode) getFieldValue(stmt, "bodyStmt");

            // مقداردهی اولیه متغیر حلقه
            if (localVariables.containsKey(varName)) {
                generateExpression(startExpr);
                storeVariable(varName, "INT");
            }

            Label startLabel = new Label();
            Label endLabel = new Label();
            currentMv.visitLabel(startLabel);

            // شرط خروج حلقه
            generateExpression(new VariableNode(varName));
            generateExpression(endExpr);
            currentMv.visitJumpInsn(IF_ICMPGE, endLabel);

            // اجرای بدنه حلقه
            if (bodyStmt != null) visitBlock(bodyStmt);

            // آپدیت متغیر
            generateExpression(stepExpr);
            storeVariable(varName, "INT");

            currentMv.visitJumpInsn(GOTO, startLabel);
            currentMv.visitLabel(endLabel);
        }
    }

    private boolean isFloatExpr(ASTNode expr) {
        if (expr instanceof FloatNode) return true;
        if (expr instanceof VariableNode) {
            if (localVariables.containsKey(((VariableNode)expr).name)) {
                return localVariables.get(((VariableNode)expr).name).type.equals("FLOAT");
            }
        }
        if (expr instanceof BinOpNode) {
            return isFloatExpr(((BinOpNode)expr).left) || isFloatExpr(((BinOpNode)expr).right);
        }
        return false;
    }

    private void generateExpression(ASTNode expr) {
        if (expr instanceof IntegerNode) {
            currentMv.visitLdcInsn(((IntegerNode) expr).value);
        } else if (expr instanceof FloatNode) {
            currentMv.visitLdcInsn((float) ((FloatNode) expr).value);
        } else if (expr instanceof StringNode) {
            currentMv.visitLdcInsn(((StringNode) expr).value);
        } else if (expr instanceof BooleanNode) {
            // برای اطمینان از اینکه به متغیرهای true/false هم گیر نمی‌دهد
            Boolean val = (Boolean) getFieldValue(expr, "value");
            if (val == null) val = ((BooleanNode) expr).value;
            currentMv.visitInsn(val ? ICONST_1 : ICONST_0);
        } else if (expr instanceof VariableNode) {
            String varName = ((VariableNode) expr).name;
            if (varName.contains(".")) {
                String[] parts = varName.split("\\.");
                LocalVar v = localVariables.get(parts[0]);
                if (v != null) {
                    currentMv.visitVarInsn(ALOAD, v.index);
                    currentMv.visitFieldInsn(GETFIELD, v.type, parts[1], "I");
                }
            }
            else if (localVariables.containsKey(varName)) {
                LocalVar v = localVariables.get(varName);
                if (v.type.equals("INT") || v.type.equals("BOOL")) currentMv.visitVarInsn(ILOAD, v.index);
                else if (v.type.equals("FLOAT")) currentMv.visitVarInsn(FLOAD, v.index);
                else currentMv.visitVarInsn(ALOAD, v.index);
            }
        } else if (expr instanceof FunctionCallNode) {
            FunctionCallNode call = (FunctionCallNode) expr;
            if (call.functionName.equals("readInt")) {
                currentMv.visitTypeInsn(NEW, "java/util/Scanner");
                currentMv.visitInsn(DUP);
                currentMv.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;");
                currentMv.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);
                currentMv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false);
            }
            else if (Character.isUpperCase(call.functionName.charAt(0))) {
                currentMv.visitTypeInsn(NEW, call.functionName);
                currentMv.visitInsn(DUP);
                for (ASTNode arg : call.arguments) {
                    generateExpression(arg);
                }
                currentMv.visitMethodInsn(INVOKESPECIAL, call.functionName, "<init>", "(I)V", false);
            }
        } else if (expr instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) expr;
            boolean isFloat = isFloatExpr(binOp);

            // --- پشتیبانی از عملگرهای شرطی (>, <, ==) برای تست 8 ---
            if (binOp.operator.matches("==|=/=|!=|<|>|<=|>=")) {
                generateExpression(binOp.left);
                generateExpression(binOp.right);
                Label trueLabel = new Label();
                Label endLabel = new Label();

                if (isFloat) {
                    currentMv.visitInsn(FCMPG);
                    switch (binOp.operator) {
                        case "==": currentMv.visitJumpInsn(IFEQ, trueLabel); break;
                        case "=/=": case "!=": currentMv.visitJumpInsn(IFNE, trueLabel); break;
                        case "<": currentMv.visitJumpInsn(IFLT, trueLabel); break;
                        case ">": currentMv.visitJumpInsn(IFGT, trueLabel); break;
                        case "<=": currentMv.visitJumpInsn(IFLE, trueLabel); break;
                        case ">=": currentMv.visitJumpInsn(IFGE, trueLabel); break;
                    }
                } else {
                    switch (binOp.operator) {
                        case "==": currentMv.visitJumpInsn(IF_ICMPEQ, trueLabel); break;
                        case "=/=": case "!=": currentMv.visitJumpInsn(IF_ICMPNE, trueLabel); break;
                        case "<": currentMv.visitJumpInsn(IF_ICMPLT, trueLabel); break;
                        case ">": currentMv.visitJumpInsn(IF_ICMPGT, trueLabel); break;
                        case "<=": currentMv.visitJumpInsn(IF_ICMPLE, trueLabel); break;
                        case ">=": currentMv.visitJumpInsn(IF_ICMPGE, trueLabel); break;
                    }
                }
                currentMv.visitInsn(ICONST_0); // False
                currentMv.visitJumpInsn(GOTO, endLabel);
                currentMv.visitLabel(trueLabel);
                currentMv.visitInsn(ICONST_1); // True
                currentMv.visitLabel(endLabel);
                return;
            }
            // --------------------------------------------------------

            generateExpression(binOp.left);
            if (isFloat && !isFloatExpr(binOp.left)) currentMv.visitInsn(I2F);

            generateExpression(binOp.right);
            if (isFloat && !isFloatExpr(binOp.right)) currentMv.visitInsn(I2F);

            if (isFloat) {
                switch (binOp.operator) {
                    case "+": currentMv.visitInsn(FADD); break;
                    case "-": currentMv.visitInsn(FSUB); break;
                    case "*": currentMv.visitInsn(FMUL); break;
                    case "/": currentMv.visitInsn(FDIV); break;
                }
            } else {
                switch (binOp.operator) {
                    case "+": currentMv.visitInsn(IADD); break;
                    case "-": currentMv.visitInsn(ISUB); break;
                    case "*": currentMv.visitInsn(IMUL); break;
                    case "/": currentMv.visitInsn(IDIV); break;
                    case "%": currentMv.visitInsn(IREM); break;
                }
            }
        }
    }

    private void storeVariable(String varName, String type) {
        int index = localVariables.get(varName).index;
        if (type.equals("INT") || type.equals("BOOL")) currentMv.visitVarInsn(ISTORE, index);
        else if (type.equals("FLOAT")) currentMv.visitVarInsn(FSTORE, index);
        else currentMv.visitVarInsn(ASTORE, index);
    }

    private String getDescriptorForType(String type) {
        switch (type) {
            case "INT": return "I";
            case "FLOAT": return "F";
            case "BOOL": return "Z";
            case "STRING": return "Ljava/lang/String;";
            default: return "L" + type + ";";
        }
    }

    private void saveClassFile(byte[] bytecode, String className) {
        try {
            String fileName = className.endsWith(".class") ? className : className + ".class";
            File outFile = new File(outputDirectory, fileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(bytecode);
            }
            System.out.println("Code Generation Successful! File saved at: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error writing class file: " + e.getMessage());
        }
    }
}