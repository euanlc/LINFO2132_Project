package compiler.Parser;


public class BinOpNode extends ASTNode {
    String operator;
    ASTNode left;
    ASTNode right;

    public BinOpNode(String operator, ASTNode left, ASTNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "ArithmeticOperator, " + operator);
        left.print(indent + "  ");
        right.print(indent + "  ");
    }
}