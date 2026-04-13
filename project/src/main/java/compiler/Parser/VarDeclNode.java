package compiler.Parser;

public class VarDeclNode extends ASTNode {
    // متغیرها به صورت public این بالا تعریف می‌شن
    public String varType;
    public String varName;
    public ASTNode expression;

    public VarDeclNode(String varType, String varName, ASTNode expression) {
        this.varType = varType;
        this.varName = varName;
        this.expression = expression;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "VarDecl, " + varType + " " + varName);
        if (expression != null) {
            expression.print(indent + "  ");
        }
    }
}