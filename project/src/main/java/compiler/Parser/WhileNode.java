package compiler.Parser;

public class WhileNode extends ASTNode {
    ASTNode condition;
    BlockNode body;

    public WhileNode(ASTNode condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "WhileStatement");
        System.out.println(indent + "  Condition");
        condition.print(indent + "    ");
        System.out.println(indent + "  Body");
        body.print(indent + "    ");
    }
}