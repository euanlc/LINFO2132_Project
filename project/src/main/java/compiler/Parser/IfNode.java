package compiler.Parser;

public class IfNode extends ASTNode {
    public ASTNode condition;
    public BlockNode thenBlock;
    public BlockNode elseBlock;

    public IfNode(ASTNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "If");
        if (condition != null) condition.print(indent + "  ");
        if (thenBlock != null) thenBlock.print(indent + "  ");
        if (elseBlock != null) elseBlock.print(indent + "  ");
    }
}