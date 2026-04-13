package compiler.Parser;

public class WhileNode extends ASTNode {
    public ASTNode condition;
    public BlockNode bodyStmt;

    public WhileNode(ASTNode condition, BlockNode bodyStmt) {
        this.condition = condition;
        this.bodyStmt = bodyStmt;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "While");
        if (condition != null) condition.print(indent + "  ");
        if (bodyStmt != null) bodyStmt.print(indent + "  ");
    }
}