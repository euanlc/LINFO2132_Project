package compiler.Parser;

public class BinOpNode extends ASTNode {
    public String operator;
    public ASTNode left;
    public ASTNode right;

    public BinOpNode(String operator, ASTNode left, ASTNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "BinOp, " + operator);
        if (left != null) left.print(indent + "  ");
        if (right != null) right.print(indent + "  ");
    }
}