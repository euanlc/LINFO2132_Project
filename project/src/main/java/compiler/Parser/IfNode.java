package compiler.Parser;

public class IfNode extends ASTNode {
    ASTNode condition;
    BlockNode thenBlock;
    BlockNode elseBlock; // This can be null if there is no 'else'

    public IfNode(ASTNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "IfStatement");
        System.out.println(indent + "  Condition");
        condition.print(indent + "    ");
        System.out.println(indent + "  ThenBlock");
        thenBlock.print(indent + "    ");
        if (elseBlock != null) {
            System.out.println(indent + "  ElseBlock");
            elseBlock.print(indent + "    ");
        }
    }
}