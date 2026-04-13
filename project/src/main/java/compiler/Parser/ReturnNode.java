package compiler.Parser;

public class ReturnNode extends ASTNode {
    public ASTNode expression;

    public ReturnNode(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public void print(String prefix) {
        System.out.println(prefix + "Return");
        if (expression != null) expression.print(prefix + "  ");
    }
}