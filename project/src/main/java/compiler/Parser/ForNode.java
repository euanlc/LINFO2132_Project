package compiler.Parser;

public class ForNode extends ASTNode {
    String varName;
    ASTNode startExpr;
    ASTNode endExpr;
    ASTNode stepExpr;
    BlockNode body;

    public ForNode(String varName, ASTNode startExpr, ASTNode endExpr, ASTNode stepExpr, BlockNode body) {
        this.varName = varName;
        this.startExpr = startExpr;
        this.endExpr = endExpr;
        this.stepExpr = stepExpr;
        this.body = body;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "ForStatement");
        System.out.println(indent + "  Variable, " + varName);
        System.out.println(indent + "  Start");
        startExpr.print(indent + "    ");
        System.out.println(indent + "  End");
        endExpr.print(indent + "    ");
        System.out.println(indent + "  Step");
        stepExpr.print(indent + "    ");
        System.out.println(indent + "  Body");
        body.print(indent + "    ");
    }
}